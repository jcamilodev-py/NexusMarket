# Cart Services

## Introduction

This document defines the services belonging to the **Cart Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Adding product variants to the buyer's active cart.
- Changing the quantity of a cart line.
- Removing lines from the cart.
- Consulting the cart.

The confirmation of the cart — its conversion into an order — is performed by **Place Order** (`order-services.md`), because that is the moment the commercial commitment begins.

**Source:** OBJ-07 ("Gestionar el carrito de compras"); DOMINIO 7 (stage "Carrito: Selección provisional de productos"); Sección 6.1 step 5 ("El comprador selecciona productos mediante el carrito y confirma el pedido").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

```text
Buyer
└── activeCart : Cart
                  ├── identifier
                  ├── buyer        : Buyer
                  ├── cartItems    : List<CartItem>
                  │                    ├── cart     : Cart
                  │                    ├── variant  : ProductVariant
                  │                    └── quantity > 0
                  └── creationDate
```

A cart carries no commercial commitment: it freezes no price and reserves no inventory (*Domain Model*, `Cart`). It always reflects the current catalog price.

Every buyer has exactly one active cart, opened at registration (`buyer-services.md`) and replaced by a new empty one each time an order is placed.

---

# Service Design Principles

## Domain Model Parameters

### Incorrect

```java
addToCart(
    String buyerId,
    String variantId,
    int quantity
);
```

### Correct

```java
addItemToCart(User requestingUser, CartItem item);
```

The line carries the variant and the quantity. The cart is never received: it is always the active cart of the requesting buyer, which makes it impossible for a buyer to act on another buyer's cart.

## The Requesting User

Every service receives the requesting `User`, who must be a buyer.

---

# Domain Behavior

| Method                                                           | Rule it enforces                                                                                                   |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `CartItem Cart.addItem(ProductVariant variant, int quantity)`    | `quantity > 0`; the variant's product is `PUBLISHED`; the same variant appears at most once — adding it again increases the existing line; the cart holds a single currency. |
| `int Cart.updateItemQuantity(ProductVariant variant, int quantity)` | The line exists; `quantity > 0` and different from the current one. Returns the previous quantity.              |
| `CartItem Cart.removeItem(ProductVariant variant)`               | The line exists. Returns the removed line.                                                                        |
| `boolean Cart.isEmpty()`                                         | Used by **Place Order**, which rejects an empty cart.                                                             |
| `BigDecimal Cart.calculateTotal()`                               | Sum of quantity × current product price of every line.                                                            |

**Inferred — a cart holds a single currency.** An `Order` has one `currency` and one `totalAmount` (*Domain Model*), and a cart becomes an order at checkout. A cart mixing products priced in different currencies could never be confirmed, so the rule is enforced when the line is added rather than discovered at checkout.

---

# 1. Add Item to Cart

## Description

Adds a variant of a published product to the buyer's active cart. Adding a variant already present increases the quantity of the existing line.

Stock is not checked here: the cart reserves nothing, and availability is decided when the order is placed (*Domain Model*, `Cart`).

**Performed by:** Buyer, over their own cart.

**Source:** OBJ-07; Sección 6.1 step 5; DOMINIO 5 (only published products are visible).

---

## Input

```text
requestingUser : User
item           : CartItem   variant (variantId), quantity
```

---

## Processing

```text
requestingUser + item
 │
 ▼
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
CartRepositoryPort.findActiveByBuyer ──> active cart
 │
 ▼
ProductRepositoryPort.findVariantById ──> variant with its product
 │
 ▼
cart.addItem(variant, quantity)
 │
 ▼
CartRepositoryPort.update
 │
 ▼
Register Operation and Audit (CART_ITEM_ADDITION)
```

---

## Operation and Audit

| Field                | Value                                         |
| -------------------- | --------------------------------------------- |
| `operationType`      | `CART_ITEM_ADDITION`                          |
| `affectedEntityType` | `CART`                                        |
| `affectedEntityId`   | `identifier` of the cart                      |
| `performedBy`        | the buyer                                     |
| `details`            | `sku`, `quantityAdded`, `lineQuantity`        |

---

# 2. Update Cart Item Quantity

## Description

Changes the quantity of a line already in the cart. To remove a line, **Remove Item from Cart** is used; a quantity of zero is not accepted.

The product does not need to be published: a buyer can still lower the quantity of a line whose product was suspended after it was added.

**Performed by:** Buyer, over their own cart.

**Source:** OBJ-07.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
CartRepositoryPort.findActiveByBuyer
 │
 ▼
previous = cart.updateItemQuantity(item.variant, item.quantity)
 │
 ▼
CartRepositoryPort.update
 │
 ▼
Register Operation and Audit (CART_ITEM_UPDATE)
```

---

## Operation and Audit

`CART_ITEM_UPDATE`, `CART`, cart `identifier`, performed by the buyer, details `sku`, `previousQuantity`, `newQuantity`.

---

# 3. Remove Item from Cart

## Description

Removes a line from the buyer's active cart.

**Performed by:** Buyer, over their own cart.

**Source:** OBJ-07.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
CartRepositoryPort.findActiveByBuyer
 │
 ▼
removed = cart.removeItem(item.variant)
 │
 ▼
CartRepositoryPort.update
 │
 ▼
Register Operation and Audit (CART_ITEM_REMOVAL)
```

---

## Operation and Audit

`CART_ITEM_REMOVAL`, `CART`, cart `identifier`, performed by the buyer, details `sku`, `removedQuantity`.

---

# 4. Consult Cart

## Description

Retrieves the buyer's active cart with its lines, valued at the current catalog prices through `Cart.calculateTotal()`.

**Performed by:** Buyer, over their own cart.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
CartRepositoryPort.findActiveByBuyer
 │
 ▼
Cart
```

Consultation changes nothing and generates no `Operation`.

---

# Output Ports

```text
CartRepositoryPort
ProductRepositoryPort   (defined in catalog-services.md)
```

---

# CartRepositoryPort

## Contract

```java
public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findActiveByBuyer(User buyer);

    void update(Cart cart);
}
```

`findActiveByBuyer` returns the buyer's active cart with its lines, their variants, and the variants' products. It receives the requesting `User` — the authoritative user returned by **Validate User Status** — so the cart can only be the requester's own. `update` persists the lines as they are: added, changed, and removed.

---

# Validation Matrix

| Service                   | Requesting user | Role  | Cart                      | Variant                          |
| ------------------------- | --------------- | ----- | ------------------------- | -------------------------------- |
| Add Item to Cart          | ACTIVE          | BUYER | Requester's active cart   | Exists; product published; same currency |
| Update Cart Item Quantity | ACTIVE          | BUYER | Requester's active cart   | Line exists                      |
| Remove Item from Cart     | ACTIVE          | BUYER | Requester's active cart   | Line exists                      |
| Consult Cart              | ACTIVE          | BUYER | Requester's active cart   | —                                |

## Service-to-Port Matrix

| Service                   | CartRepositoryPort              | ProductRepositoryPort |
| ------------------------- | ------------------------------- | --------------------- |
| Add Item to Cart          | `findActiveByBuyer`, `update`   | `findVariantById`     |
| Update Cart Item Quantity | `findActiveByBuyer`, `update`   |                       |
| Remove Item from Cart     | `findActiveByBuyer`, `update`   |                       |
| Consult Cart              | `findActiveByBuyer`             |                       |

---

# Input Ports

All four services are exposed to the Buyer.

---

# Exceptions

| Exception                        | Raised when                                                                                         |
| -------------------------------- | --------------------------------------------------------------------------------------------------- |
| `InvalidCartException`           | The line is missing its variant or quantity; the quantity is not positive or unchanged; the product is not published; the currency differs from the cart's; the line to update or remove is not in the cart. |
| `EntityNotFoundException`        | The variant or the active cart does not exist.                                                      |
| `UnauthorizedOperationException` | The requesting user is not an `ACTIVE` buyer.                                                        |

---

# Business Rules Summary

## BR-CRT-001 — A buyer only acts on their own active cart

The cart is resolved from the requester, never received. (DOMINIO 2; RG-03)

## BR-CRT-002 — Only variants of published products are added

(DOMINIO 5; *Domain Model*, `Cart`)

## BR-CRT-003 — A variant appears at most once per cart

Adding it again increases the existing line. (*Domain Model*, `CartItem`)

## BR-CRT-004 — Quantities are always greater than zero

(*Domain Model*, `CartItem`)

## BR-CRT-005 — The cart reserves no inventory and freezes no price

(*Domain Model*, `Cart`)

## BR-CRT-006 — A cart holds a single currency

Inferred; see **Domain Behavior**.

## BR-CRT-007 — A commercially restricted buyer may still manage their cart

The restriction applies to placing orders. (DOMINIO 2)

---

# Java Implementation

```text
domain/
├── models/
│   └── Cart.java                          addItem, updateItemQuantity, removeItem, isEmpty, calculateTotal
├── exceptions/
│   └── InvalidCartException.java
├── ports/out/
│   └── CartRepositoryPort.java            completed
└── services/cart/
    ├── AddItemToCartService.java          Cart addItemToCart(User requestingUser, CartItem item)
    ├── UpdateCartItemQuantityService.java Cart updateCartItemQuantity(User requestingUser, CartItem item)
    ├── RemoveItemFromCartService.java     Cart removeItemFromCart(User requestingUser, CartItem item)
    └── ConsultCartService.java            Cart consultCart(User requestingUser)
```

---

# Architectural Constraints

1. `Cart` and `CartItem` are Domain Models; line rules live in `Cart`.
2. Services receive Domain Models, never primitive identifiers, DTOs, or persistence entities.
3. The cart is always resolved from the requesting buyer.
4. Every state change registers an `Operation` and its `AuditLog`.
5. All cart rules must remain testable without infrastructure.
