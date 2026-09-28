# Order Services

## Introduction

This document defines the services belonging to the **Order Management** subdomain of the NexusMarket marketplace platform.

The order is the formal commercial commitment of the buyer, and its lifecycle is the central process of the system (DOMINIO 7). The services in this subdomain are responsible for:

- Placing an order from the buyer's active cart.
- Cancelling an unpaid order.
- Consulting orders.
- Moving the order through its lifecycle as payment, dispatch, and delivery happen.

**Source:** OBJ-08 ("Controlar el ciclo completo de los pedidos"); DOMINIO 7. Gestión de Pedidos ("Representa el compromiso comercial formal. Su ciclo de vida es el proceso central del sistema"); Sección 6.1 steps 5–8; Sección 11 ("Un pedido finalizado no podrá ser modificado bajo ninguna circunstancia"); Matriz de Responsabilidades ("Gestión de Pedidos → Comprador, Vendedor, Op. Logístico").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

```text
Order
├── identifier
├── buyer            : Buyer
├── orderItems       : List<OrderItem>
│                        ├── variant          : ProductVariant
│                        ├── quantity, unitPrice, subtotal      frozen at checkout
│                        └── sourceInventory  : Inventory       physical lines only
├── shippingAddress                           copied at checkout
├── orderStatus      : OrderStatus
├── creationDate
├── totalAmount, currency
├── payments         : List<Payment>
├── invoice          : Invoice
└── shipments        : List<Shipment>
```

The lifecycle, defined in *Domain Value Objects*, is:

```text
PENDING_PAYMENT ──> PAID ──> DISPATCHED ──> DELIVERED
      │               │
      └─> CANCELLED   └─> DELIVERED        (orders with digital lines only)
```

`DELIVERED` and `CANCELLED` are terminal; a `DELIVERED` order can no longer be modified (Sección 11).

Within an order, a line is identified by its variant: the cart merges each variant into a single line, and the order copies the cart's lines.

---

# Service Design Principles

### Incorrect

```java
placeOrder(String buyerId, String shippingAddress);
cancelOrder(String orderId);
```

### Correct

```java
placeOrder(User requestingUser, Order order);      // order carries the chosen shippingAddress
cancelOrder(User requestingUser, Order order);     // order carries its identifier
```

The lines are never received: they are copied from the buyer's active cart, so the buyer cannot alter prices or add lines outside the cart.

The internal services receive the `User` whose business action triggered them — the buyer who pays, the logistics operator who dispatches or delivers.

---

# Domain Behavior

| Method                                                   | Rule it enforces                                                                                                   |
| -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `static Order Order.placeFrom(Cart cart, String shippingAddress)` | The cart has lines; every variant belongs to a published product; the address is one of the buyer's addresses; copies each line freezing `unitPrice` and `subtotal`; computes `totalAmount` and takes the cart's currency; starts in `PENDING_PAYMENT`. |
| `boolean Order.hasPhysicalItems()`                       | Whether any line requires inventory and dispatch.                                                                 |
| `List<OrderItem> Order.unshippedPhysicalItemsFrom(Warehouse warehouse)` | The physical lines reserved from that warehouse and not yet in a shipment (`shipment-services.md`).  |
| `boolean Order.containsProductsOf(User seller)`          | Whether any line belongs to a product of that seller.                                                             |
| `void Order.cancel()`                                    | Only from `PENDING_PAYMENT`.                                                                                      |
| `void Order.confirmPayment()`                            | Only from `PENDING_PAYMENT`.                                                                                      |
| `void Order.markDispatched()`                            | Only from `PAID`, and only when the order has physical lines.                                                     |
| `boolean Order.isReadyForDelivery()`                     | `PAID` with no physical lines, or `DISPATCHED` with every physical line in a delivered shipment.                  |
| `void Order.markDelivered()`                             | Only when `isReadyForDelivery()`.                                                                                 |

Every transition not listed is rejected, which is how Sección 11 — a finished order can no longer be modified — is enforced: `DELIVERED` has no outgoing transition.

---

# 1. Place Order

## Description

Confirms the buyer's active cart and converts it into an order in state `PENDING_PAYMENT`.

The service freezes the unit price of each line and the delivery address, reserves inventory for every physical line, closes the confirmed cart, and opens a new empty cart for the buyer.

**Performed by:** Buyer.

**Source:** Sección 6.1 step 5 ("El comprador selecciona productos mediante el carrito y confirma el pedido"); DOMINIO 7; DOMINIO 2 (commercial status); DOMINIO 6 (reservation).

---

## Input

```text
requestingUser : User
order          : Order    shippingAddress
```

---

## Domain Validations

* The buyer's commercial status is `ENABLED` (`Buyer.canPlaceOrders()`, DOMINIO 2).
* The active cart is not empty.
* Every variant in the cart still belongs to a `PUBLISHED` product: a product suspended or discontinued after it was added to the cart can no longer be ordered.
* The shipping address is the buyer's primary address or one of their additional addresses (DOMINIO 2 defines where a buyer receives deliveries).
* Every physical line can be reserved in full from a single inventory record (`inventory-services.md`, **Reserve Inventory**).

---

## Processing

```text
requestingUser + order
 │
 ▼
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
buyer.canPlaceOrders()? ──no──> InvalidOrderException
 │
 ▼
CartRepositoryPort.findActiveByBuyer ──> cart
 │
 ▼
newOrder = Order.placeFrom(cart, order.shippingAddress)
 │
 ▼
OrderRepositoryPort.save ──> order (identifier assigned)
 │
 ▼
For each physical line: Reserve Inventory (buyer, line)    sets line.sourceInventory
 │
 ▼
OrderRepositoryPort.update
 │
 ▼
Cart.openFor(buyer) ──> CartRepositoryPort.save ──> buyer.assignActiveCart ──> BuyerRepositoryPort.update
 │
 ▼
Register Operation and Audit (CART_CONFIRMATION)
 │
 ▼
Register Operation and Audit (ORDER_PLACEMENT)
```

The confirmed cart is no longer the buyer's active cart and is never modified again, so the order it produced remains reproducible (*Domain Model*, `Cart`).

---

## Transactional Consistency

The order, every reservation, the new cart, and the operations must all succeed or all fail. If a single line cannot be reserved, the whole placement fails and every reservation already made is rolled back with it: an order never exists with part of its stock reserved.

---

## Operation and Audit

| Field                | Cart operation                     | Order operation                              |
| -------------------- | ---------------------------------- | -------------------------------------------- |
| `operationType`      | `CART_CONFIRMATION`                | `ORDER_PLACEMENT`                            |
| `affectedEntityType` | `CART`                             | `ORDER`                                      |
| `affectedEntityId`   | confirmed cart `identifier`        | order `identifier`                           |
| `performedBy`        | the buyer                          | the buyer                                    |
| `details`            | `orderId`                          | `totalAmount`, `currency`, `itemCount`       |

Each reservation registers its own `INVENTORY_RESERVATION` operation.

---

# 2. Consult Order

## Description

Retrieves an order with its lines, payments, invoice, and shipments.

**Performed by:** The buyer who placed it; a seller over the orders that contain their products; Logistics Operator, Administrator, and Supervisor over any order.

**Source:** Matriz de Responsabilidades ("Gestión de Pedidos → Comprador, Vendedor, Op. Logístico"); RG-03.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER, SELLER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR)
 │
 ▼
OrderRepositoryPort.findById ──> stored order? ──no──> EntityNotFoundException
 │
 ├── BUYER  ──> Validate Buyer Ownership (order.buyer)
 └── SELLER ──> order.containsProductsOf(seller)? ──no──> UnauthorizedOperationException
 │
 ▼
Order
```

A seller sees the whole order, not only their own lines: the specification gives the seller a role in order management without distinguishing lines, and the order is a single commercial commitment.

---

# 3. Consult Orders

## Description

Retrieves the orders a user may see, optionally filtered by status.

| Requesting role                                     | Orders returned                                  |
| --------------------------------------------------- | ------------------------------------------------ |
| Buyer                                               | Their own orders                                 |
| Seller                                              | Orders containing their products                 |
| Logistics Operator, Administrator, Supervisor       | All orders                                       |

**Performed by:** The same participants as **Consult Order**.

---

## Input

```text
requestingUser : User
criteria       : Order    optional orderStatus
```

---

## Processing

```text
Validate User Status ──> Validate Role Permission
 │
 ├── BUYER  ──> OrderRepositoryPort.findByBuyer
 ├── SELLER ──> OrderRepositoryPort.findBySeller
 └── others ──> OrderRepositoryPort.findByStatus(criteria)    all orders when no status is given
 │
 ▼
Filter by criteria.orderStatus when present
 │
 ▼
List<Order>
```

Consultations change nothing and generate no `Operation`.

---

# 4. Cancel Order

## Description

Cancels an order in state `PENDING_PAYMENT`, releasing every inventory reservation it holds.

An order can no longer be cancelled once paid: the buyer's money has been collected, and the only reimbursement path the specification describes is a refund originated by a return (OBJ-11).

**Performed by:** The buyer who placed it. Inferred: the specification does not describe cancellation; `CANCELLED` itself is an inferred state, and the buyer is the party of the commercial commitment.

**Source:** *Domain Value Objects*, `OrderStatus.CANCELLED`; *Domain Model*, `Order`.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
OrderRepositoryPort.findById ──> Validate Buyer Ownership
 │
 ▼
order.cancel()                          rejects anything but PENDING_PAYMENT
 │
 ▼
For each physical line: Release Inventory Reservation (buyer, line)
 │
 ▼
OrderRepositoryPort.update
 │
 ▼
Register Operation and Audit (ORDER_CANCELLATION)
```

---

## Operation and Audit

`ORDER_CANCELLATION`, `ORDER`, order `identifier`, performed by the buyer, details `releasedLineCount`. Each release registers its own `INVENTORY_RESERVATION_RELEASE` operation.

---

# 5. Confirm Order Payment

## Description

Moves an order from `PENDING_PAYMENT` to `PAID` when one of its payment attempts is approved, and triggers the issuance of its invoice.

An order composed exclusively of digital lines is then delivered immediately, since digital products are delivered as soon as payment is confirmed (DOMINIO 5).

**Performed by:** Internal — triggered by **Register Payment** (`payment-billing-services.md`) when the attempt is approved, on behalf of the buyer.

**Source:** Sección 6.1 step 6 ("Se valida el pago y se inicia el flujo de preparación"); DOMINIO 5; DOMINIO 7.

---

## Processing

```text
order.confirmPayment()
 │
 ▼
OrderRepositoryPort.update
 │
 ▼
Register Operation and Audit (ORDER_PAYMENT_CONFIRMATION)
 │
 ▼
Issue Invoice (buyer, order)
 │
 ▼
Mark Order Delivered (buyer, order)        delivers only a digital-only order
```

---

# 6. Mark Order Dispatched

## Description

Moves an order from `PAID` to `DISPATCHED` when its **first** shipment leaves the warehouse. Later shipments of the same order change nothing at order level.

**Performed by:** Internal — triggered by **Dispatch Shipment**, on behalf of the logistics operator.

**Source:** DOMINIO 7 ("Despachado: Salida física de la bodega"); *Domain Model*, `Order` (mixed orders).

---

## Processing

```text
order is PAID? ──no──> nothing to do
 │
 ▼
order.markDispatched() ──> OrderRepositoryPort.update ──> Register Operation and Audit (ORDER_DISPATCH)
```

---

# 7. Mark Order Delivered

## Description

Moves an order to `DELIVERED` when it is ready for delivery: every physical line is in a delivered shipment, or the order has no physical lines. From that moment the order can no longer be modified (Sección 11).

**Performed by:** Internal — triggered by **Confirm Shipment Delivery** and by **Confirm Order Payment**.

**Source:** DOMINIO 7 ("Entregado / Finalizado"); Sección 6.1 step 8 ("El pedido se marca como finalizado tras la entrega confirmada"); Sección 11.

---

## Processing

```text
order.isReadyForDelivery()? ──no──> nothing to do
 │
 ▼
order.markDelivered() ──> OrderRepositoryPort.update ──> Register Operation and Audit (ORDER_DELIVERY)
```

---

# Output Ports

```text
OrderRepositoryPort
CartRepositoryPort      (cart-services.md)
BuyerRepositoryPort     (buyer-services.md)
```

---

# OrderRepositoryPort

## Contract

```java
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(Order order);

    List<Order> findByBuyer(User buyer);

    List<Order> findBySeller(User seller);

    List<Order> findByStatus(Order criteria);

    void update(Order order);
}
```

Lookups return the order with its buyer, lines (with variant, product, seller, and source inventory), payments, invoice, and shipments. `findByStatus` returns every order when `criteria.orderStatus` is absent. `update` persists the status and the lines' source inventory; the lines themselves never change after creation.

---

# Validation Matrix

| Service               | Requesting user | Role                                                     | Ownership                                    | State rule                                      |
| --------------------- | --------------- | -------------------------------------------------------- | -------------------------------------------- | ----------------------------------------------- |
| Place Order           | ACTIVE          | BUYER                                                    | Own active cart                              | ENABLED buyer; cart not empty; published products; own address; stock |
| Consult Order         | ACTIVE          | BUYER, SELLER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR | Buyer: own order. Seller: contains their products | —                                        |
| Consult Orders        | ACTIVE          | Same                                                     | Resolved by role                             | —                                               |
| Cancel Order          | ACTIVE          | BUYER                                                    | Own order                                    | PENDING_PAYMENT                                 |
| Confirm Order Payment | Internal        | —                                                        | —                                            | PENDING_PAYMENT                                 |
| Mark Order Dispatched | Internal        | —                                                        | —                                            | PAID, first shipment                            |
| Mark Order Delivered  | Internal        | —                                                        | —                                            | Ready for delivery                              |

## Service-to-Port Matrix

| Service               | OrderRepositoryPort                                  | CartRepositoryPort              | BuyerRepositoryPort | Internal services invoked                          |
| --------------------- | ---------------------------------------------------- | ------------------------------- | ------------------- | -------------------------------------------------- |
| Place Order           | `save`, `update`                                     | `findActiveByBuyer`, `save`     | `update`            | Reserve Inventory                                  |
| Consult Order         | `findById`                                           |                                 |                     |                                                    |
| Consult Orders        | `findByBuyer`, `findBySeller`, `findByStatus`        |                                 |                     |                                                    |
| Cancel Order          | `findById`, `update`                                 |                                 |                     | Release Inventory Reservation                      |
| Confirm Order Payment | `update`                                             |                                 |                     | Issue Invoice, Mark Order Delivered                |
| Mark Order Dispatched | `update`                                             |                                 |                     |                                                    |
| Mark Order Delivered  | `update`                                             |                                 |                     |                                                    |

---

# Input Ports

| Service                                                  | Exposed to                                                     |
| -------------------------------------------------------- | -------------------------------------------------------------- |
| Place Order, Cancel Order                                | Buyer                                                          |
| Consult Order, Consult Orders                            | Buyer, Seller, Logistics Operator, Administrator, Supervisor   |
| Confirm Order Payment, Mark Order Dispatched, Mark Order Delivered | Internal — not exposed                              |

---

# Exceptions

| Exception                          | Raised when                                                                                                  |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `InvalidOrderException`            | The buyer is commercially restricted; the cart is empty; a product is no longer published; the address is not one of the buyer's. |
| `InsufficientStockException`       | A physical line cannot be reserved (`inventory-services.md`).                                                |
| `InvalidStatusTransitionException` | A lifecycle transition is requested from a status that does not allow it — including any change to a `DELIVERED` or `CANCELLED` order. |
| `EntityNotFoundException`          | The order or the active cart does not exist.                                                                 |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, lacks the required role, or does not own or participate in the order.   |

---

# Business Rules Summary

## BR-ORD-001 — An order is created only from the buyer's confirmed active cart

(Sección 6.1 step 5)

## BR-ORD-002 — Only an ENABLED buyer places orders

(DOMINIO 2)

## BR-ORD-003 — Only published products are ordered

(DOMINIO 5)

## BR-ORD-004 — Prices and address are frozen at checkout

(*Domain Model*, **Frozen Commercial Conditions**)

## BR-ORD-005 — Every physical line is reserved in full from one inventory record

Otherwise the order is not placed. (DOMINIO 6; *Domain Model*, `OrderItem.sourceInventory`)

## BR-ORD-006 — Placing an order opens a new empty cart

(*Domain Model*, `Buyer`)

## BR-ORD-007 — Only an unpaid order can be cancelled, and cancellation releases its reservations

(*Domain Value Objects*, `OrderStatus`)

## BR-ORD-008 — The order follows its lifecycle; a delivered order can never change

(DOMINIO 7; Sección 11)

## BR-ORD-009 — A digital-only order is delivered as soon as it is paid

(DOMINIO 5)

## BR-ORD-010 — An order with physical lines is delivered only when every shipment is delivered

(Sección 6.1 step 8)

---

# Java Implementation

```text
domain/
├── models/
│   └── Order.java                         placeFrom, hasPhysicalItems, unshippedPhysicalItemsFrom, containsProductsOf, cancel, confirmPayment, markDispatched, isReadyForDelivery, markDelivered
├── exceptions/
│   └── InvalidOrderException.java
├── ports/out/
│   └── OrderRepositoryPort.java
└── services/order/
    ├── PlaceOrderService.java             Order placeOrder(User requestingUser, Order order)
    ├── ConsultOrderService.java           Order consultOrder(User requestingUser, Order order)
    ├── ConsultOrdersService.java          List<Order> consultOrders(User requestingUser, Order criteria)
    ├── CancelOrderService.java            Order cancelOrder(User requestingUser, Order order)
    ├── ConfirmOrderPaymentService.java    Order execute(User performedBy, Order order)
    ├── MarkOrderDispatchedService.java    Order execute(User performedBy, Order order)
    └── MarkOrderDeliveredService.java     Order execute(User performedBy, Order order)
```

---

# Architectural Constraints

1. `Order` and `OrderItem` are Domain Models; lifecycle rules live in `Order`.
2. Services receive Domain Models, never primitive identifiers, DTOs, or persistence entities.
3. Order lines come only from the buyer's active cart.
4. Lines are never modified after the order is created.
5. Status changes go through `Order` behavior and follow the `OrderStatus` lifecycle.
6. Every state change registers an `Operation` and its `AuditLog`.
7. All order rules must remain testable without infrastructure.
