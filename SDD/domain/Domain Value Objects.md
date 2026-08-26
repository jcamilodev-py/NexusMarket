# Domain Value Objects

## Introduction

Value Objects represent immutable concepts within the NexusMarket domain.

Unlike Entities, Value Objects do not have their own identity. They are defined entirely by their values and are used to encapsulate controlled business concepts, improve domain expressiveness, and prevent the use of primitive values or scattered string literals throughout the application.

The marketplace domain uses Value Objects for business catalogs such as roles, statuses, product types, movement types, operation types, affected entity types, and currencies.

All business catalogs inherit from `DomainCatalog`.

Where the specification names a catalog but only gives partial examples of its values — for instance "Activo, Bloqueado, etc." — or names the concept without listing values at all, the remaining values are inferred from the business context, mirroring how the banking reference model was built from prose rather than from exhaustive tables. Each such catalog states its rationale in its own section, so that the reasoning travels inside the document rather than in a separate review list.

---

# Value Object Hierarchy

```text
DomainCatalog (Abstract)
├── SystemRole
├── UserStatus
├── BuyerCommercialStatus
├── ProductType
├── ProductStatus
├── InventoryStatus
├── InventoryMovementType
├── OrderStatus
├── PaymentStatus
├── ShipmentStatus
├── ReturnStatus
├── RefundStatus
├── OperationType
├── AffectedEntityType
└── Currency
```

---

# DomainCatalog (Abstract)

## Description

Represents a generic business catalog used throughout the NexusMarket domain.

`DomainCatalog` provides a consistent structure for controlled business values that require a code, a human-readable name, and a business description.

All controlled business values inherit from this class, ensuring a consistent structure across the application.

This class cannot be instantiated directly.

## Attributes

| Attribute   | Type   | Description                                           |
| ----------- | ------ | ----------------------------------------------------- |
| code        | String | Unique business identifier of the catalog value.      |
| name        | String | Human-readable name displayed within the application. |
| description | String | Business definition of the catalog value.             |

## Characteristics

* Immutable.
* Equality is determined by value rather than by object identity.
* Catalog values are controlled by the domain.
* Catalog values must not be represented by arbitrary strings throughout the application.
* Each catalog value must have a unique `code`.

---

# SystemRole

## Description

Represents the responsibilities and permissions assigned to a participant within the marketplace.

The role is a characteristic of `User` because it represents what the participant means within the system and the responsibilities associated with them. Each user holds exactly one role, and that role is what determines which `User` specialization applies.

The `role` attribute is therefore defined in `User` and inherited by its specializations.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** Sección 5. Participantes del Negocio; RG-02 ("Cada usuario tendrá un único rol dentro del sistema"). All five values are stated literally in the specification.

| Code               | Name               | Description                                                      |
| ------------------ | ------------------ | ---------------------------------------------------------------- |
| BUYER              | Buyer              | Purchases products published on the marketplace.                 |
| SELLER             | Seller             | Registers and manages their own products.                        |
| LOGISTICS_OPERATOR | Logistics Operator | Manages the physical operation of warehouses and dispatches.     |
| ADMINISTRATOR      | Administrator      | Manages sellers, warehouses, and refunds.                        |
| SUPERVISOR         | Supervisor         | Consultation and operational follow-up profile, with read-only access. |

---

# UserStatus

## Description

Represents whether a user can operate normally within the platform.

`UserStatus` describes the state of the participant's access to the system. It is independent from `BuyerCommercialStatus`, which describes a buyer's ability to purchase: a buyer may be an active user of the platform and still be commercially restricted.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** DOMINIO 1 — "Estado: Condición operativa (Activo, Bloqueado, etc.)". `INACTIVE` is inferred to complete the catalog, mirroring the equivalent catalog in the banking reference: a user who is no longer active but was never sanctioned (`BLOCKED`) is a distinct and common business condition, for example a seller account under review or an employee on leave. The specification's own "etc." indicates the list it gives is not exhaustive.

| Code     | Name     | Description                                       |
| -------- | -------- | ------------------------------------------------- |
| ACTIVE   | Active   | User can access the system normally.              |
| INACTIVE | Inactive | User exists but cannot perform system operations. |
| BLOCKED  | Blocked  | User access has been suspended.                   |

---

# BuyerCommercialStatus

## Description

Represents the buyer's condition for placing purchases.

This catalog is independent from `UserStatus`. A buyer whose commercial status is not `ENABLED` cannot confirm an order, even though their platform access remains active.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** DOMINIO 2 — "Estado comercial: Condición del comprador para realizar compras." The specification names the attribute but does not enumerate its values. Inferred as a two-state catalog: the existence of a commercial status separate from the user status only makes sense if a buyer can be prevented from purchasing without their account being blocked at the `UserStatus` level — that is, for commercial reasons rather than security ones. A binary distinction is the minimal shape that gives the attribute meaning.

| Code       | Name       | Description                                       |
| ---------- | ---------- | ------------------------------------------------- |
| ENABLED    | Enabled    | Buyer can place orders normally.                  |
| RESTRICTED | Restricted | Buyer is temporarily prevented from purchasing.   |

---

# ProductType

## Description

Represents whether a product is physical or digital, determining whether it requires inventory and shipping.

This catalog is carried as an attribute of `Product` in addition to the `PhysicalProduct` / `DigitalProduct` specialization, so that the catalog can be filtered and validated without relying on the runtime type of the object.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** DOMINIO 5 — "Tipo de Producto: Físico o Digital." Both values are stated literally in the specification.

| Code     | Name     | Description                                                            |
| -------- | -------- | ---------------------------------------------------------------------- |
| PHYSICAL | Physical | Requires inventory tracking and physical dispatch through a warehouse. |
| DIGITAL  | Digital  | Delivered immediately upon payment confirmation.                       |

---

# ProductStatus

## Description

Represents the publication state of a product in the catalog.

Only a product in state `PUBLISHED` is visible in the public catalog and may be added to a cart.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** DOMINIO 5 — "Estado: Publicado, Suspendido o Descontinuado." All three values are stated literally in the specification.

| Code         | Name         | Description                              |
| ------------ | ------------ | ---------------------------------------- |
| PUBLISHED    | Published    | Visible in the public catalog.           |
| SUSPENDED    | Suspended    | Temporarily hidden from the catalog.     |
| DISCONTINUED | Discontinued | Permanently removed from active sale.    |

## Lifecycle

```text
PUBLISHED
    │
    ├──────────────> SUSPENDED
    │                    │
    │                    └──────> PUBLISHED
    │
    └──────────────> DISCONTINUED
```

`SUSPENDED` is reversible; `DISCONTINUED` is terminal.

---

# InventoryStatus

## Description

Represents the condition of a stock record, used to determine whether it is eligible for reservation.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** Sección 11 — "No se puede reservar inventario inexistente o marcado como 'Dañado'." `AVAILABLE` is inferred as the necessary counterpart to `DAMAGED`: the validation rule only has meaning as a binary distinction between stock that can and stock that cannot be reserved.

| Code      | Name      | Description                                             |
| --------- | --------- | ------------------------------------------------------- |
| AVAILABLE | Available | Stock can be normally reserved and sold.                |
| DAMAGED   | Damaged   | Stock is marked as damaged and cannot be reserved.      |

---

# InventoryMovementType

## Description

Represents the type of change applied to an inventory record.

Inventory movements are the historical record of every stock variation, while `Inventory` holds the resulting current quantities.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** DOMINIO 6 — "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución." All five values are stated literally in the specification.

| Code          | Name          | Description                                            |
| ------------- | ------------- | ------------------------------------------------------ |
| INBOUND       | Inbound       | Stock entering the warehouse.                          |
| RESERVATION   | Reservation   | Stock reserved for a pending order.                    |
| SALE_OUTBOUND | Sale Outbound | Stock leaving the warehouse due to a confirmed sale.   |
| ADJUSTMENT    | Adjustment    | Manual correction of the stock quantity.               |
| RETURN        | Return        | Stock re-entering due to an approved product return.   |

---

# OrderStatus

## Description

Represents the current stage of an order's lifecycle.

The status describes the current state of the order, while its operations provide the historical record of the actions performed throughout its lifecycle.

**Note on the `CART` value.** DOMINIO 7 lists "Carrito" as the first stage of the order lifecycle. In this model that stage is materialized by the `Cart` entity, which is separate from `Order` because OBJ-07 and OBJ-08 are two distinct functional objectives. As a consequence, **no `Order` instance ever holds the value `CART`**: an order is created directly in `PENDING_PAYMENT`. The value is nevertheless kept in the catalog so that the catalog remains faithful to the five stages the specification enumerates literally, and so that a reviewer comparing the specification against this document finds all of them.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** DOMINIO 7 — "Ciclo de Estados del Pedido"; Sección 11 ("Un pedido finalizado no podrá ser modificado"). `CANCELLED` is inferred: the specification enumerates only the successful path, but an order whose `Payment` never reaches `APPROVED` requires a terminal state — without it, an unpaid order would remain in `PENDING_PAYMENT` indefinitely and the `REJECTED` and `FAILED` values of `PaymentStatus` would have no consequence at the order level.

| Code            | Name                | Description                                                                                                                            |
| --------------- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| CART            | Cart                | Provisional selection of products. Stage represented by the `Cart` entity; no `Order` instance holds this value. Kept for fidelity to the state list of DOMINIO 7. |
| PENDING_PAYMENT | Pending Payment     | Order created, awaiting financial confirmation.                                                                                        |
| PAID            | Paid                | Payment confirmed; the fulfilment process begins and the invoice is issued.                                                            |
| DISPATCHED      | Dispatched          | The first shipment of the order has physically left the warehouse.                                                                     |
| DELIVERED       | Delivered / Finished | Every shipment has been delivered; the order is closed and can no longer be modified.                                                  |
| CANCELLED       | Cancelled           | Order terminated without completing delivery. Inferred; see the source note above.                                                     |

## Lifecycle

```text
CART                      (stage held by the Cart entity)
  │
  │ cart confirmation
  ▼
PENDING_PAYMENT
  │
  ├──────────────> CANCELLED
  │
  ▼
PAID
  │
  ├──────────────> CANCELLED
  │
  ├──────────────> DELIVERED   (orders composed exclusively of digital lines)
  │
  ▼
DISPATCHED
  │
  ▼
DELIVERED
```

`DELIVERED` and `CANCELLED` are terminal. An order in `DELIVERED` can no longer be modified under any circumstance (Sección 11).

---

# PaymentStatus

## Description

Represents the state of a payment attempt registered against an order.

Each attempt to collect the amount of an order is its own `Payment` entity with its own status, so that a rejected attempt remains as a historical record rather than being overwritten by a later retry.

An order moves to `PAID` only when one of its payment attempts reaches `APPROVED`.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** DOMINIO 7 (states "Pendiente de Pago" and "Pagado"); Sección 6.1 step 6 — "Se valida el pago y se inicia el flujo de preparación." The specification requires the payment to be validated but does not enumerate the possible outcomes of that validation, so the values are inferred: a validation that can only succeed would not be a validation. `REJECTED` and `FAILED` are distinguished because they have different business meanings — a commercial refusal is a decision, whereas a technical failure is an incident, and only the former reflects on the buyer.

| Code     | Name     | Description                                                                                     |
| -------- | -------- | ----------------------------------------------------------------------------------------------- |
| PENDING  | Pending  | Payment attempt registered, awaiting validation.                                                |
| APPROVED | Approved | Payment validated successfully; the order moves to `PAID`.                                      |
| REJECTED | Rejected | Payment refused during validation. The order is unaffected and a new attempt may be registered. |
| FAILED   | Failed   | Payment could not be completed due to a processing error rather than a refusal.                 |

## Lifecycle

```text
PENDING
   │
   ├──────────────> REJECTED
   │
   ├──────────────> FAILED
   │
   ▼
APPROVED
```

`APPROVED`, `REJECTED`, and `FAILED` are terminal for a given attempt. A rejected or failed attempt never modifies the order; a new `Payment` is created for each retry.

---

# ShipmentStatus

## Description

Represents the current execution stage of a shipment.

A single order may produce several shipments when its physical lines are stocked in different warehouses, and each of them carries its own status.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** OBJ-10; Sección 4.1 ("Envíos: Procesos logísticos para productos físicos"); Sección 6.1 step 7, which describes packing, dispatch, and transport as sequential sub-steps. The specification names the process but does not enumerate its states, so the values below are inferred by mapping directly onto that sequence, at the same granularity `OrderStatus` uses for its own dispatch and delivery stages.

| Code       | Name       | Description                                                                  |
| ---------- | ---------- | ---------------------------------------------------------------------------- |
| PENDING    | Pending    | Shipment created and being packed; it has not yet left the warehouse.        |
| IN_TRANSIT | In Transit | Shipment dispatched from the warehouse and en route to the buyer.            |
| DELIVERED  | Delivered  | Delivery confirmed. When every shipment of an order reaches this state, the order moves to `DELIVERED`. |

## Lifecycle

```text
PENDING
   │
   │ dispatch  (generates an InventoryMovement of type SALE_OUTBOUND)
   ▼
IN_TRANSIT
   │
   │ delivery confirmation
   ▼
DELIVERED
```

---

# ReturnStatus

## Description

Represents the current stage of a return request.

The return concerns the product being sent back; the money movement that follows an approved return is represented separately by `RefundStatus`.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** OBJ-11, which names "devoluciones" as a process requiring administration; Matriz de Responsabilidades, where the buyer initiates the process. The specification does not enumerate the states, so the values below are inferred following the request → decision → closure pattern that the specification itself uses for approval-driven flows, and that the banking reference uses for its own request-based products.

| Code      | Name      | Description                                                                            |
| --------- | --------- | -------------------------------------------------------------------------------------- |
| REQUESTED | Requested | Return request created by the buyer, awaiting review.                                  |
| APPROVED  | Approved  | Return accepted; it originates a `Refund` and, for physical items, a stock return.     |
| REJECTED  | Rejected  | Return request denied. No refund is originated.                                        |
| COMPLETED | Completed | Returned product received and processed; the request is closed.                        |

## Lifecycle

```text
REQUESTED
    │
    ├──────────────> REJECTED
    │
    ▼
 APPROVED
    │
    │ Refund created + InventoryMovement of type RETURN
    ▼
COMPLETED
```

`REJECTED` and `COMPLETED` are terminal.

---

# RefundStatus

## Description

Represents the current stage of a monetary refund.

`Refund` is the financial counterpart of `Payment`: one records money leaving the marketplace, the other money entering it.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** OBJ-11, which names "reembolsos" as a process administered separately from returns; Matriz de Responsabilidades ("Gestión Reembolsos → Admin ejecuta"). The specification does not enumerate the states, so the values below are inferred, following the same execution pattern as `PaymentStatus` in the opposite direction.

| Code      | Name      | Description                                                          |
| --------- | --------- | -------------------------------------------------------------------- |
| PENDING   | Pending   | Refund originated by an approved return, not yet executed.           |
| PROCESSED | Processed | Funds returned to the buyer by an administrator.                     |
| REJECTED  | Rejected  | Refund denied during administrative review.                          |

## Lifecycle

```text
PENDING
   │
   ├──────────────> REJECTED
   │
   ▼
PROCESSED
```

---

# OperationType

## Description

Represents the type of significant business operation executed within the marketplace.

Operations represent business events or actions performed over the entities of the domain. Every significant business action must reference an `OperationType`, and every operation must be recorded in the `AuditLog`.

Operations are independent from entity statuses:

* A **status** represents the current state of an entity.
* An **operation** represents an action or event that occurred.

For example:

```text
Order.orderStatus = PAID
```

represents the current state of the order, while:

```text
Operation.operationType = ORDER_PAYMENT_CONFIRMATION
```

represents the event that caused the order to become paid.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** Derived from the business events identified across the specification — user and seller onboarding (DOMINIO 1, DOMINIO 3, Sección 6.1 step 1), warehouse administration (DOMINIO 4), catalog publication (DOMINIO 5, Sección 6.1 steps 2 and 4), inventory movements (DOMINIO 6), the cart and order lifecycle (DOMINIO 7, OBJ-07, OBJ-08, Sección 6.1 steps 5–8), billing (OBJ-09), logistics (OBJ-10), and the return/refund flow (OBJ-11) — so that the `AuditLog` covers the full traceability scope required by OBJ-12 and by the commitment stated in Sección 1.

The values are grouped by the business area they belong to. Each value corresponds to an action listed in the *Examples of Generated Operations* of the entities in the *Domain Model*.

### User Operations

| Code                | Name                | Description                                                    |
| ------------------- | ------------------- | -------------------------------------------------------------- |
| USER_REGISTRATION   | User Registration   | A user was registered in the platform.                         |
| USER_STATUS_CHANGE  | User Status Change  | The operational status of a user was modified.                 |
| BUYER_REGISTRATION  | Buyer Registration  | A buyer was registered in the platform.                        |
| SELLER_REGISTRATION | Seller Registration | A seller was onboarded by an administrator, with their first warehouse. |

### Warehouse Operations

| Code                    | Name                    | Description                            |
| ----------------------- | ----------------------- | -------------------------------------- |
| WAREHOUSE_REGISTRATION  | Warehouse Registration  | A warehouse was registered.            |

### Catalog Operations

| Code                     | Name                     | Description                                          |
| ------------------------ | ------------------------ | ---------------------------------------------------- |
| PRODUCT_REGISTRATION     | Product Registration     | A product was registered by its seller.              |
| PRODUCT_PUBLICATION      | Product Publication      | A product was published to the public catalog.       |
| PRODUCT_SUSPENSION       | Product Suspension       | A product was temporarily hidden from the catalog.   |
| PRODUCT_DISCONTINUATION  | Product Discontinuation  | A product was permanently removed from active sale.  |

### Inventory Operations

| Code                    | Name                    | Description                                                  |
| ----------------------- | ----------------------- | ------------------------------------------------------------ |
| INVENTORY_INBOUND       | Inventory Inbound       | Stock entered a warehouse.                                   |
| INVENTORY_RESERVATION   | Inventory Reservation   | Stock was reserved for an order.                             |
| INVENTORY_SALE_OUTBOUND | Inventory Sale Outbound | Stock left a warehouse due to a confirmed sale.              |
| INVENTORY_ADJUSTMENT    | Inventory Adjustment    | Stock quantity was manually corrected.                       |
| INVENTORY_RETURN        | Inventory Return        | Stock re-entered a warehouse due to an approved return.      |

### Cart Operations

| Code                     | Name                     | Description                                          |
| ------------------------ | ------------------------ | ---------------------------------------------------- |
| CART_ITEM_ADDITION       | Cart Item Addition       | A product variant was added to a cart.               |
| CART_ITEM_REMOVAL        | Cart Item Removal        | A product variant was removed from a cart.           |
| CART_CONFIRMATION        | Cart Confirmation        | A cart was confirmed, producing an order.            |

### Order Operations

| Code                       | Name                       | Description                                                    |
| -------------------------- | -------------------------- | -------------------------------------------------------------- |
| ORDER_PLACEMENT            | Order Placement            | An order was created from a confirmed cart.                    |
| ORDER_PAYMENT_CONFIRMATION | Order Payment Confirmation | Payment for an order was confirmed and the order became paid.  |
| ORDER_DISPATCH             | Order Dispatch             | The order was dispatched, following its first shipment.        |
| ORDER_DELIVERY             | Order Delivery             | Every shipment of the order was delivered; the order is closed. |
| ORDER_CANCELLATION         | Order Cancellation         | The order was terminated without completing delivery.          |

### Payment Operations

| Code                 | Name                 | Description                                            |
| -------------------- | -------------------- | ------------------------------------------------------ |
| PAYMENT_REGISTRATION | Payment Registration | A payment attempt was registered against an order.     |
| PAYMENT_APPROVAL     | Payment Approval     | A payment attempt was validated successfully.          |
| PAYMENT_REJECTION    | Payment Rejection    | A payment attempt was refused or failed.               |

### Billing Operations

| Code             | Name             | Description                                      |
| ---------------- | ---------------- | ------------------------------------------------ |
| INVOICE_ISSUANCE | Invoice Issuance | An invoice was issued for a paid order.          |

### Shipment Operations

| Code              | Name              | Description                                             |
| ----------------- | ----------------- | ------------------------------------------------------- |
| SHIPMENT_CREATION | Shipment Creation | A shipment was created for the physical lines of an order. |
| SHIPMENT_DISPATCH | Shipment Dispatch | A shipment left a warehouse.                            |
| SHIPMENT_DELIVERY | Shipment Delivery | A shipment was confirmed as delivered.                  |

### Return and Refund Operations

| Code                    | Name                    | Description                                                  |
| ----------------------- | ----------------------- | ------------------------------------------------------------ |
| RETURN_REQUEST_CREATION | Return Request Creation | A buyer requested the return of one or more purchased items. |
| RETURN_APPROVAL         | Return Approval         | A return request was accepted.                               |
| RETURN_REJECTION        | Return Rejection        | A return request was denied.                                 |
| RETURN_COMPLETION       | Return Completion       | A returned product was received and the request was closed.  |
| REFUND_PROCESSING       | Refund Processing       | A refund was executed by an administrator.                   |
| REFUND_REJECTION        | Refund Rejection        | A refund was denied during administrative review.            |

---

# AffectedEntityType

## Description

Represents the type of business entity affected by an `Operation`.

**Design decision — why this catalog exists.** The banking reference points `Operation.affectedProduct` at `BankingProduct`, an abstract root shared by all of its products. NexusMarket has no equivalent business concept: significant events affect entities as different as `Order`, `Payment`, `Inventory`, `Shipment`, `ReturnRequest`, `Refund`, `Product`, `Seller`, and `Warehouse`, which share no common business meaning. Introducing an artificial technical root to group them would invent a concept the specification does not support, and would contradict the design rule that inheritance represents genuine domain specialization.

Instead, `Operation` and `AuditLog` qualify the affected entity with this catalog plus the entity's identifier. The reference stays typed and controlled by the domain, without a false hierarchy and without the arbitrary strings that the Design Notes forbid.

## Inherits From

`DomainCatalog`

## Allowed Values

**Source:** Derived from the entities of the *Domain Model* that generate operations, as listed in their *Examples of Generated Operations*. Each value corresponds to one entity or family of entities that a business event can affect.

| Code           | Name           | Description                                                                       |
| -------------- | -------------- | --------------------------------------------------------------------------------- |
| USER           | User           | Identity and access events over any participant of the platform.                  |
| SELLER         | Seller         | Commercial onboarding and administration of a vendor. Kept separate from `USER` because the specification treats seller registration as its own business process (DOMINIO 3, OBJ-02) with its own responsible role. |
| WAREHOUSE      | Warehouse      | Registration and administration of a storage location.                            |
| PRODUCT        | Product        | Catalog events over a product and its variants.                                   |
| INVENTORY      | Inventory      | Stock events over an inventory record.                                            |
| CART           | Cart           | Events over a buyer's provisional selection.                                      |
| ORDER          | Order          | Events over the lifecycle of an order.                                            |
| PAYMENT        | Payment        | Events over a payment attempt.                                                    |
| INVOICE        | Invoice        | Events over a commercial billing document.                                        |
| SHIPMENT       | Shipment       | Events over the logistics fulfilment of an order.                                 |
| RETURN_REQUEST | Return Request | Events over a return request and its lines.                                       |
| REFUND         | Refund         | Events over a monetary reimbursement.                                             |

---

# Currency

## Description

Represents a monetary currency supported by the marketplace.

Currency is a business Value Object because its meaning is determined by its controlled values rather than by an independent identity.

**Design decision — why `Currency` applies to NexusMarket.** The specification never names a currency, which could suggest the concept is out of scope. It is not: the marketplace intermediates between independent third-party sellers and buyers, issues invoices (OBJ-09), collects payments (Sección 6.1 step 6), and returns money through refunds (OBJ-11). A `BigDecimal` with no denomination becomes ambiguous the moment more than one currency exists, and a refunded amount must be provably the same amount that was charged — which cannot be verified if neither side is denominated. The banking reference establishes `Currency` as a `DomainCatalog` Value Object for exactly this reason, and the same argument holds here. Modeling it now costs one Value Object; omitting it would require changing every monetary attribute of the model later.

Every entity holding a monetary amount also holds its currency: `Product`, `Order`, `Payment`, `Invoice`, and `Refund`.

## Inherits From

`DomainCatalog`

## Additional Attributes

| Attribute | Type   | Description                       |
| --------- | ------ | --------------------------------- |
| isoCode   | String | ISO 4217 currency code.           |
| symbol    | String | Currency symbol used for display. |

## Allowed Values

**Source:** Inferred. The specification enumerates no currencies, so the catalog is seeded with the same values as the banking reference. This is deliberate rather than arbitrary: the purpose of the Value Object is that supporting an additional currency becomes a catalog entry rather than a change to the domain model, so the initial set only needs to be coherent, not exhaustive.

| ISO Code | Name                 | Symbol |
| -------- | -------------------- | ------ |
| COP      | Colombian Peso       | $      |
| USD      | United States Dollar | $      |
| EUR      | Euro                 | €      |

---

# Primitive Enumerations

The following concepts would be represented as simple enumerations, because they contain fixed technical values and do not require business catalog metadata such as `code`, `name`, or `description`.

**None were identified from the NexusMarket specification.**

Unlike the banking reference, the source document gives no basis for technical-only concepts such as notification channels or audit severity levels: it explicitly places interfaces, authentication mechanisms, implementation technologies, and storage details outside its scope (Sección 3.2). Every catalog defined above ties back to an explicit business concept in the specification, so all of them are modeled as `DomainCatalog` Value Objects.

Approval outcomes — which the banking reference models as a primitive `ApprovalDecision` enumeration — are not modeled separately here either, because in this domain each approval-driven flow already carries its decision within its own business catalog: `ReturnStatus` for returns, `RefundStatus` for refunds, and `PaymentStatus` for payments. Adding a generic decision enumeration would duplicate information that the domain already expresses with more precision.

---

# Design Notes

## Inheritance

All business catalogs inherit from `DomainCatalog`, which guarantees that every controlled value carries a `code`, a `name`, and a `description`.

## Immutability

All Value Objects are immutable after creation. Their values cannot be modified once the object has been instantiated.

## Equality

Value Objects are compared according to their values rather than their object identity. Two instances containing the same business values represent the same Value Object.

## Controlled Values

Business catalogs must use controlled values defined by the domain. The application must avoid replacing these concepts with arbitrary strings such as:

```text
"ACTIVE"
"PAID"
"DAMAGED"
```

throughout the codebase. Instead, the corresponding Value Object must be used:

```text
UserStatus
OrderStatus
InventoryStatus
```

## Business Versus Technical Enumerations

A business concept is modeled as a `DomainCatalog` Value Object when it requires a business code, a display name, a business description, and controlled domain evolution. A simple enumeration is used when the concept represents a fixed technical value without additional business metadata. No such concept was identified in this specification; see the section above.

## Statuses Versus Operations

A **status** catalog represents the current state of an entity — `OrderStatus`, `PaymentStatus`, `ShipmentStatus`. An **operation** catalog represents an action or event that occurred — `OperationType`. The two are kept separate throughout the domain: `Order.orderStatus = PAID` describes where the order is now, while `Operation.operationType = ORDER_PAYMENT_CONFIRMATION` describes what happened to bring it there.

## Independent Status Catalogs

Statuses that describe different concerns are modeled as separate catalogs and never collapsed into one: `UserStatus` describes platform access, while `BuyerCommercialStatus` describes the ability to purchase. A buyer may be an active user and still be commercially restricted.

## Relationship With Entities

Entities reference Value Objects rather than primitive strings whenever the referenced value represents a controlled business concept:

```text
User.role                       : SystemRole
User.status                     : UserStatus
Buyer.commercialStatus          : BuyerCommercialStatus
Product.productType             : ProductType
Product.productStatus           : ProductStatus
Product.currency                : Currency
Inventory.inventoryStatus       : InventoryStatus
InventoryMovement.movementType  : InventoryMovementType
Order.orderStatus               : OrderStatus
Order.currency                  : Currency
Payment.paymentStatus           : PaymentStatus
Payment.currency                : Currency
Invoice.currency                : Currency
Shipment.shipmentStatus         : ShipmentStatus
ReturnRequest.returnStatus      : ReturnStatus
Refund.refundStatus             : RefundStatus
Refund.currency                 : Currency
Operation.operationType         : OperationType
Operation.affectedEntityType    : AffectedEntityType
AuditLog.operationType          : OperationType
AuditLog.userRole               : SystemRole
AuditLog.affectedEntityType     : AffectedEntityType
```

## Traceability to the Specification

Every catalog states the section of the *Especificación Funcional del Negocio* its values originate from, and every catalog whose values are not fully enumerated in the specification states its inference rationale directly in its own section. This is a deliberate addition over the banking reference: the reasoning that produced the catalog travels with the artifact rather than requiring a separate review list.

## Result

This approach improves type safety, domain expressiveness, maintainability, and consistency with Domain-Driven Design principles, while supporting future evolution of the domain: adding a currency, a status, or an operation type is a catalog entry, not a change to the model.
