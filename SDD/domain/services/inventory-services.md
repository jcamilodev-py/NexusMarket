# Inventory Services

## Introduction

This document defines the services belonging to the **Inventory Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Registering stock entering a warehouse.
- Correcting stock quantities.
- Marking stock as damaged or available.
- Reserving, releasing, and removing stock as orders are placed, cancelled, and dispatched.
- Returning stock received back from buyers.
- Consulting stock and its movement history.

**Source:** OBJ-06 ("Administrar el inventario distribuido"); DOMINIO 6. Gestión del Inventario ("El inventario es distribuido y debe estar vinculado obligatoriamente a un producto y una bodega específica"; "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución"; "No se permitirán existencias negativas bajo ninguna circunstancia"); Sección 11 ("No se puede reservar inventario inexistente o marcado como 'Dañado'"); Matriz de Responsabilidades ("Administración Inventario → Vendedor, Op. Logístico"); DOMINIO 2 ("El comprador nunca administrará información de otros compradores ni inventarios").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

```text
Inventory
├── identifier
├── variant           : ProductVariant   of a PhysicalProduct only
├── warehouse         : Warehouse
├── availableQuantity                    never negative
├── reservedQuantity                     never negative
└── inventoryStatus   : InventoryStatus  AVAILABLE | DAMAGED

InventoryMovement
├── identifier
├── inventory     : Inventory
├── movementType  : InventoryMovementType
├── quantity
├── movementDate
└── performedBy   : User
```

There is exactly one `Inventory` record per variant and warehouse. The inventory is **distributed**: the same variant may have records in several warehouses.

Every change to `availableQuantity` or `reservedQuantity` is recorded as an `InventoryMovement`. The effect of each movement type is:

| Movement              | available | reserved | Triggered by                           |
| --------------------- | --------- | -------- | -------------------------------------- |
| `INBOUND`             | `+ q`     |          | Register Inventory Inbound             |
| `ADJUSTMENT`          | `± q`     |          | Adjust Inventory                       |
| `RESERVATION`         | `− q`     | `+ q`    | Place Order (`order-services.md`)      |
| `RESERVATION_RELEASE` | `+ q`     | `− q`    | Cancel Order (`order-services.md`)     |
| `SALE_OUTBOUND`       |           | `− q`    | Dispatch Shipment (`shipment-services.md`) |
| `RETURN`              | `+ q`     |          | Complete Return (`return-refund-services.md`) |

`quantity` is always positive, except for `ADJUSTMENT`, where it carries the sign of the correction.

---

# Service Design Principles

## Domain Model Parameters

### Incorrect

```java
registerInbound(
    String variantId,
    String warehouseId,
    int quantity
);
```

### Correct

```java
registerInventoryInbound(User requestingUser, InventoryMovement inbound);
```

The movement carries its quantity and the inventory it affects — a variant and a warehouse, each identified by its Domain Model. The service builds the actual movement from the stored records; the one it receives is only the request.

## The Requesting User

The services exposed to participants receive the requesting `User` first. The internal services receive the `User` whose business action triggered them — the buyer who places an order, the logistics operator who dispatches a shipment — who becomes the `performedBy` of the movement and of the operation.

---

# Domain Behavior

| Method                                                              | Rule it enforces                                                                                                   |
| ------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `static Inventory Inventory.open(ProductVariant variant, Warehouse warehouse)` | Only for a variant of a `PhysicalProduct` (DOMINIO 5); a `SellerWarehouse` only holds stock of its owner's products; starts at zero and `AVAILABLE`. |
| `InventoryMovement Inventory.receive(int quantity, User by)`        | `quantity > 0`; available increases.                                                                               |
| `InventoryMovement Inventory.adjust(int delta, User by)`            | `delta ≠ 0`; available never becomes negative.                                                                    |
| `boolean Inventory.canReserve(int quantity)`                        | `AVAILABLE` status and enough available units (Sección 11).                                                       |
| `InventoryMovement Inventory.reserve(int quantity, User by)`        | Rejects what `canReserve` rejects; moves units from available to reserved.                                        |
| `InventoryMovement Inventory.releaseReservation(int quantity, User by)` | Reserved units are enough; moves them back to available.                                                     |
| `InventoryMovement Inventory.registerSaleOutbound(int quantity, User by)` | Reserved units are enough; removes them.                                                                   |
| `InventoryMovement Inventory.registerReturn(int quantity, User by)` | `quantity > 0`; available increases.                                                                               |
| `void Inventory.changeStatus(InventoryStatus newStatus)`            | Rejects a missing status and a change to the current one.                                                          |
| `static InventoryMovement InventoryMovement.record(Inventory inventory, InventoryMovementType type, int quantity, User by)` | Builds a complete, immutable movement stamped with the current moment. |

Every quantity-changing method returns the movement it produced, so a service cannot change stock without obtaining the record of that change.

The non-negativity of both quantities (DOMINIO 6) is therefore enforced by the entity itself, before anything is persisted.

---

# 1. Register Inventory Inbound

## Description

Registers stock entering a warehouse for a variant of a physical product. If the variant has no inventory record in that warehouse yet, the record is created first — this is how a variant gets its first stock, and what allows its product to be published (`catalog-services.md`).

**Performed by:** Seller, over the inventory of their own products; Logistics Operator, over any inventory.

**Source:** DOMINIO 6 ("Ingreso"); Sección 6.1 step 3 ("Se registran existencias iniciales en las bodegas asociadas"); Matriz de Responsabilidades.

---

## Input

```text
requestingUser : User
inbound        : InventoryMovement   quantity; inventory.variant (variantId); inventory.warehouse (identifier)
```

---

## Processing

```text
requestingUser + inbound
 │
 ▼
Validate User Status ──> Validate Role Permission (SELLER, LOGISTICS_OPERATOR)
 │
 ▼
ProductRepositoryPort.findVariantById ──> variant with its product and seller
 │
 ▼
WarehouseRepositoryPort.findById ──> warehouse
 │
 ▼
Requesting user holds SELLER? ──yes──> Validate Seller Ownership (variant.product.seller)
 │
 ▼
InventoryRepositoryPort.findByVariantAndWarehouse
 │
 ├── found ──> inventory
 └── absent ─> Inventory.open(variant, warehouse) ──> InventoryRepositoryPort.save
 │
 ▼
movement = inventory.receive(quantity, user)
 │
 ▼
InventoryRepositoryPort.update ──> InventoryMovementRepositoryPort.save
 │
 ▼
Register Operation and Audit (INVENTORY_INBOUND)
```

A seller may register stock of their own products in their own warehouses and in marketplace warehouses, which may hold stock of any seller (*Domain Model*, `MarketplaceWarehouse`). A seller warehouse only holds stock of its owner's products, so a logistics operator cannot place another seller's stock in it either.

---

## Operation and Audit

| Field                | Value                                                  |
| -------------------- | ------------------------------------------------------ |
| `operationType`      | `INVENTORY_INBOUND`                                    |
| `affectedEntityType` | `INVENTORY`                                            |
| `affectedEntityId`   | `identifier` of the inventory record                   |
| `performedBy`        | the seller or logistics operator                       |
| `details`            | `quantity`, `sku`, `warehouseId`, `availableAfter`     |

---

# 2. Adjust Inventory

## Description

Applies a manual correction to the available quantity of an inventory record — for example after a physical count. Corrections never edit previous movements; each correction is a new `ADJUSTMENT` movement.

Reserved units cannot be adjusted: they belong to orders, and are only released by cancelling the order or removed by dispatching it.

**Performed by:** Seller, over the inventory of their own products; Logistics Operator.

**Source:** DOMINIO 6 ("Ajuste"); Matriz de Responsabilidades.

---

## Input

```text
requestingUser : User
adjustment     : InventoryMovement   quantity (signed, not zero); inventory (identifier)
```

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SELLER, LOGISTICS_OPERATOR)
 │
 ▼
InventoryRepositoryPort.findById ──> inventory with variant, product, seller
 │
 ▼
Requesting user holds SELLER? ──yes──> Validate Seller Ownership
 │
 ▼
movement = inventory.adjust(quantity, user)        rejects a negative result
 │
 ▼
InventoryRepositoryPort.update ──> InventoryMovementRepositoryPort.save
 │
 ▼
Register Operation and Audit (INVENTORY_ADJUSTMENT)
```

---

## Operation and Audit

`INVENTORY_ADJUSTMENT`, `INVENTORY`, inventory `identifier`, performed by the requesting user, details `quantity` (signed) and `availableAfter`.

---

# 3. Change Inventory Status

## Description

Marks an inventory record as `DAMAGED` or returns it to `AVAILABLE`.

A damaged record cannot be reserved (Sección 11). Units already reserved from it are not affected: they belong to orders placed before the status changed, and the specification only forbids **reserving** damaged stock.

The status change moves no units, so it produces no `InventoryMovement`; it is traced by its own operation.

**Performed by:** Seller, over the inventory of their own products; Logistics Operator.

**Source:** Sección 11; Matriz de Responsabilidades.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SELLER, LOGISTICS_OPERATOR)
 │
 ▼
InventoryRepositoryPort.findById ──> seller ownership when applicable
 │
 ▼
inventory.changeStatus(target) ──> InventoryRepositoryPort.update
 │
 ▼
Register Operation and Audit (INVENTORY_STATUS_CHANGE)
```

---

## Operation and Audit

`INVENTORY_STATUS_CHANGE`, `INVENTORY`, inventory `identifier`, details `previousStatus`, `newStatus`.

---

# 4. Reserve Inventory

## Description

Reserves the stock required by one physical line of a newly placed order.

The line is reserved **in full from a single inventory record**, and that record becomes the line's `sourceInventory` (*Domain Model*, `OrderItem`). Among the records of the variant that can cover the line, the one with the most available units is chosen, so that stock is consumed from where it is most abundant.

If no single record can cover the line, the reservation fails and the whole order is rejected.

**Performed by:** Internal — triggered by **Place Order**, on behalf of the buyer who places it.

**Source:** DOMINIO 6 ("Reserva"); Sección 11; *Domain Model*, `Order` ("Creating an Order reserves inventory").

---

## Input

```text
performedBy : User        the buyer
orderItem   : OrderItem   variant, quantity, order (with identifier)
```

---

## Processing

```text
InventoryRepositoryPort.findByVariant(orderItem.variant)
 │
 ▼
Keep the records where canReserve(quantity) ──> none? ──> InsufficientStockException
 │
 ▼
Choose the one with the most available units
 │
 ▼
movement = inventory.reserve(quantity, buyer)
 │
 ▼
orderItem.sourceInventory = inventory
 │
 ▼
InventoryRepositoryPort.update ──> InventoryMovementRepositoryPort.save
 │
 ▼
Register Operation and Audit (INVENTORY_RESERVATION)
```

---

## Operation and Audit

`INVENTORY_RESERVATION`, `INVENTORY`, inventory `identifier`, performed by the buyer, details `orderId`, `quantity`.

---

# 5. Release Inventory Reservation

## Description

Returns to the available quantity the units reserved by one line of an order that is being cancelled.

**Performed by:** Internal — triggered by **Cancel Order**, on behalf of the buyer.

**Source:** *Domain Value Objects*, `InventoryMovementType.RESERVATION_RELEASE`; *Domain Model*, `Order` ("Cancelling an Order releases every inventory reservation it holds").

---

## Processing

```text
orderItem.sourceInventory ──> InventoryRepositoryPort.findById
 │
 ▼
movement = inventory.releaseReservation(quantity, buyer)
 │
 ▼
InventoryRepositoryPort.update ──> InventoryMovementRepositoryPort.save
 │
 ▼
Register Operation and Audit (INVENTORY_RESERVATION_RELEASE)
```

Details: `orderId`, `quantity`.

---

# 6. Register Sale Outbound

## Description

Removes from the reserved quantity the units of one order line that physically leave the warehouse when its shipment is dispatched.

**Performed by:** Internal — triggered by **Dispatch Shipment**, on behalf of the logistics operator.

**Source:** DOMINIO 6 ("Salida por venta"); *Domain Model*, `Shipment`.

---

## Processing

```text
orderItem.sourceInventory ──> InventoryRepositoryPort.findById
 │
 ▼
movement = inventory.registerSaleOutbound(quantity, operator)
 │
 ▼
InventoryRepositoryPort.update ──> InventoryMovementRepositoryPort.save
 │
 ▼
Register Operation and Audit (INVENTORY_SALE_OUTBOUND)
```

Details: `orderId`, `quantity`.

---

# 7. Register Inventory Return

## Description

Returns to the available quantity the units of one returned line, into the inventory record the line was sold from (`orderItem.sourceInventory`), so that returned units go back to the warehouse they left.

**Performed by:** Internal — triggered by **Complete Return**, on behalf of the logistics operator.

**Source:** DOMINIO 6 ("Devolución"); *Domain Model*, `ReturnRequest`.

---

## Input

```text
performedBy : User         the logistics operator
returnItem  : ReturnItem   quantity; orderItem with its sourceInventory
```

---

## Processing

```text
returnItem.orderItem.sourceInventory ──> InventoryRepositoryPort.findById
 │
 ▼
movement = inventory.registerReturn(quantity, operator)
 │
 ▼
InventoryRepositoryPort.update ──> InventoryMovementRepositoryPort.save
 │
 ▼
Register Operation and Audit (INVENTORY_RETURN)
```

Details: `returnRequestId`, `quantity`.

---

# 8. Consult Inventory

## Description

Retrieves the inventory records of a variant — one per warehouse that stocks it — with their available and reserved quantities and status.

**Performed by:** Seller, over the inventory of their own products; Logistics Operator, Administrator, and Supervisor over any inventory. Buyers never consult inventory (DOMINIO 2).

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SELLER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR)
 │
 ▼
ProductRepositoryPort.findVariantById ──> seller ownership when applicable
 │
 ▼
InventoryRepositoryPort.findByVariant
 │
 ▼
List<Inventory>
```

---

# 9. Consult Inventory Movements

## Description

Retrieves the history of movements applied to an inventory record.

**Performed by:** The same participants as **Consult Inventory**.

---

## Processing

```text
Validate User Status ──> Validate Role Permission
 │
 ▼
InventoryRepositoryPort.findById ──> seller ownership when applicable
 │
 ▼
InventoryMovementRepositoryPort.findByInventory
 │
 ▼
List<InventoryMovement>
```

Consultations change nothing and generate no `Operation`.

---

# Output Ports

```text
InventoryRepositoryPort
InventoryMovementRepositoryPort
ProductRepositoryPort     (defined in catalog-services.md; adds findVariantById)
WarehouseRepositoryPort   (defined in warehouse-services.md)
```

---

# InventoryRepositoryPort

## Contract

```java
public interface InventoryRepositoryPort {

    Inventory save(Inventory inventory);

    Optional<Inventory> findById(Inventory inventory);

    Optional<Inventory> findByVariantAndWarehouse(Inventory criteria);

    List<Inventory> findByVariant(ProductVariant variant);

    boolean existsByVariant(ProductVariant variant);

    void update(Inventory inventory);
}
```

Every lookup returns the record with its variant, the variant's product and seller, and its warehouse, since ownership and the physical nature of the product are decided from them.

---

# InventoryMovementRepositoryPort

## Contract

```java
public interface InventoryMovementRepositoryPort {

    InventoryMovement save(InventoryMovement movement);

    List<InventoryMovement> findByInventory(Inventory inventory);
}
```

Movements are immutable (*Domain Model*, `InventoryMovement`): the port offers no update nor delete.

---

# ProductRepositoryPort — addition

```java
Optional<ProductVariant> findVariantById(ProductVariant variant);
```

Returns the variant with its product and the product's seller.

---

# Validation Matrix

| Service                        | Requesting user | Role validation                                       | Ownership                        | Stock rule                               |
| ------------------------------ | --------------- | ----------------------------------------------------- | -------------------------------- | ---------------------------------------- |
| Register Inventory Inbound     | ACTIVE          | SELLER, LOGISTICS_OPERATOR                            | Seller: own product              | Physical product; seller warehouse of the owner |
| Adjust Inventory               | ACTIVE          | SELLER, LOGISTICS_OPERATOR                            | Seller: own product              | Available never negative                 |
| Change Inventory Status        | ACTIVE          | SELLER, LOGISTICS_OPERATOR                            | Seller: own product              | Status actually changes                  |
| Reserve Inventory              | Internal        | —                                                     | —                                | AVAILABLE and enough units in one record |
| Release Inventory Reservation  | Internal        | —                                                     | —                                | Enough reserved units                    |
| Register Sale Outbound         | Internal        | —                                                     | —                                | Enough reserved units                    |
| Register Inventory Return      | Internal        | —                                                     | —                                | Quantity positive                        |
| Consult Inventory              | ACTIVE          | SELLER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR | Seller: own product              | —                                        |
| Consult Inventory Movements    | ACTIVE          | SELLER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR | Seller: own product              | —                                        |

## Service-to-Port Matrix

| Service                        | InventoryRepositoryPort                                   | InventoryMovementRepositoryPort | ProductRepositoryPort | WarehouseRepositoryPort |
| ------------------------------ | --------------------------------------------------------- | ------------------------------- | --------------------- | ----------------------- |
| Register Inventory Inbound     | `findByVariantAndWarehouse`, `save`, `update`             | `save`                          | `findVariantById`     | `findById`              |
| Adjust Inventory               | `findById`, `update`                                      | `save`                          |                       |                         |
| Change Inventory Status        | `findById`, `update`                                      |                                 |                       |                         |
| Reserve Inventory              | `findByVariant`, `update`                                 | `save`                          |                       |                         |
| Release Inventory Reservation  | `findById`, `update`                                      | `save`                          |                       |                         |
| Register Sale Outbound         | `findById`, `update`                                      | `save`                          |                       |                         |
| Register Inventory Return      | `findById`, `update`                                      | `save`                          |                       |                         |
| Consult Inventory              | `findByVariant`                                           |                                 | `findVariantById`     |                         |
| Consult Inventory Movements    | `findById`                                                | `findByInventory`               |                       |                         |

---

# Input Ports

| Service                                                    | Exposed to                                             |
| ---------------------------------------------------------- | ------------------------------------------------------ |
| Register Inventory Inbound, Adjust Inventory, Change Inventory Status | Seller, Logistics Operator                  |
| Consult Inventory, Consult Inventory Movements             | Seller, Logistics Operator, Administrator, Supervisor  |
| Reserve, Release, Sale Outbound, Inventory Return          | Internal — not exposed                                 |

---

# Exceptions

| Exception                          | Raised when                                                                                                 |
| ---------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `InvalidInventoryException`        | The request is missing its quantity, variant, warehouse, or inventory; the variant is digital; a seller warehouse would hold another seller's stock; a quantity is not positive; an adjustment is zero. |
| `InsufficientStockException`       | A reservation cannot be covered by a single `AVAILABLE` record, or an adjustment, release, or outbound would leave a quantity negative. |
| `InvalidStatusTransitionException` | The target inventory status equals the current one.                                                         |
| `EntityNotFoundException`          | The variant, warehouse, or inventory record does not exist.                                                 |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, lacks the required role, or is a seller acting on another seller's stock. |

---

# Business Rules Summary

## BR-INV-001 — Inventory is always tied to one variant and one warehouse

(DOMINIO 6)

## BR-INV-002 — Quantities are never negative

Enforced by the `Inventory` entity itself. (DOMINIO 6)

## BR-INV-003 — Damaged or non-existent stock cannot be reserved

(Sección 11)

## BR-INV-004 — Every quantity change is an InventoryMovement

(DOMINIO 6)

## BR-INV-005 — Only physical products have inventory

(DOMINIO 5)

## BR-INV-006 — A seller warehouse only holds its owner's stock

## BR-INV-007 — Only sellers (over their own products) and logistics operators administer inventory

(Matriz de Responsabilidades)

## BR-INV-008 — Buyers never access inventory

(DOMINIO 2)

## BR-INV-009 — A line is reserved in full from one record, kept as its source

(*Domain Model*, `OrderItem.sourceInventory`)

## BR-INV-010 — Reserved units are only released by cancellation or removed by dispatch

They cannot be adjusted.

---

# Java Implementation

```text
domain/
├── models/
│   ├── Inventory.java                  open, receive, adjust, canReserve, reserve, releaseReservation, registerSaleOutbound, registerReturn, changeStatus
│   └── InventoryMovement.java          static InventoryMovement record(...)
├── exceptions/
│   ├── InvalidInventoryException.java
│   └── InsufficientStockException.java
├── ports/out/
│   ├── InventoryRepositoryPort.java
│   ├── InventoryMovementRepositoryPort.java
│   └── ProductRepositoryPort.java      + findVariantById
└── services/inventory/
    ├── RegisterInventoryInboundService.java        Inventory registerInventoryInbound(User requestingUser, InventoryMovement inbound)
    ├── AdjustInventoryService.java                 Inventory adjustInventory(User requestingUser, InventoryMovement adjustment)
    ├── ChangeInventoryStatusService.java           Inventory changeInventoryStatus(User requestingUser, Inventory inventory)
    ├── ReserveInventoryService.java                Inventory execute(User performedBy, OrderItem orderItem)
    ├── ReleaseInventoryReservationService.java     Inventory execute(User performedBy, OrderItem orderItem)
    ├── RegisterSaleOutboundService.java            Inventory execute(User performedBy, OrderItem orderItem)
    ├── RegisterInventoryReturnService.java         Inventory execute(User performedBy, ReturnItem returnItem)
    ├── ConsultInventoryService.java                List<Inventory> consultInventory(User requestingUser, ProductVariant variant)
    └── ConsultInventoryMovementsService.java       List<InventoryMovement> consultInventoryMovements(User requestingUser, Inventory inventory)
```

The internal services use `execute`, like the internal services of the banking reference; the services exposed to participants use a descriptive name.

---

# Architectural Constraints

1. `Inventory` and `InventoryMovement` are Domain Models; stock rules live in `Inventory`.
2. Services receive Domain Models, never primitive identifiers, DTOs, or persistence entities.
3. Stock is changed only through `Inventory` behavior, which always returns the movement produced.
4. Movements are immutable; corrections are new `ADJUSTMENT` movements.
5. Internal services record as performer the user whose business action triggered them.
6. Every state change registers an `Operation` and its `AuditLog`.
7. All inventory rules must remain testable without infrastructure.
