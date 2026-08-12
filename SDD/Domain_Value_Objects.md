# Domain Value Objects

## Introduction

Value Objects represent immutable concepts within the NexusMarket domain. Unlike Entities, they do not have their own identity; instead, they are defined entirely by their attributes.

These objects encapsulate controlled business values, improve domain expressiveness, and prevent the use of primitive types or scattered string literals throughout the application.

Where the specification names a catalog but only gives partial examples of its values (e.g. "Activo, Bloqueado, etc.") or names the concept without listing values at all, the remaining values are inferred from business context, mirroring how the banking reference example was built from prose rather than exhaustive tables. Each such catalog states its rationale in its own section.

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
├── ShipmentStatus
├── ReturnStatus
├── RefundStatus
└── OperationType
```

---

# DomainCatalog (Abstract)

## Description

Represents a generic business catalog used throughout the NexusMarket domain.

All controlled business values inherit from this class, ensuring a consistent structure across the application.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| code | String | Unique business identifier. |
| name | String | Human-readable name displayed within the application. |
| description | String | Business definition of the catalog value. |

---

# SystemRole

## Description

Represents the responsibilities and permissions assigned to a participant. Each user has exactly one role (RG-02).

## Inherits From

DomainCatalog

## Allowed Values

**Source:** Sección 5. Participantes del Negocio.

| Code | Name | Description |
|------|------|-------------|
| BUYER | Buyer | Purchases products published on the marketplace. |
| SELLER | Seller | Registers and manages their own products. |
| LOGISTICS_OPERATOR | Logistics Operator | Manages the physical operation of warehouses and dispatches. |
| ADMINISTRATOR | Administrator | Manages sellers and warehouses. |
| SUPERVISOR | Supervisor | Consultation and operational follow-up profile. |

---

# UserStatus

## Description

Represents whether a user can operate normally within the platform.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** DOMINIO 1 — "Estado: Condición operativa (Activo, Bloqueado, etc.)". `INACTIVE` is inferred to complete the catalog, mirroring the equivalent catalog in the banking reference: a user who is no longer active but was never sanctioned (`Blocked`) is a distinct, common business condition (e.g. a seller account under review, an employee on leave).

| Code | Name | Description |
|------|------|-------------|
| ACTIVE | Active | User can access the system normally. |
| INACTIVE | Inactive | User exists but cannot perform operations. |
| BLOCKED | Blocked | User access has been suspended. |

---

# BuyerCommercialStatus

## Description

Represents the buyer's condition for placing purchases.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** DOMINIO 2 — "Estado comercial: Condición del comprador para realizar compras." Inferred as a two-state catalog: the specification's own restriction language ("el comprador nunca administrará...") and the general emphasis on controlled access throughout the document (RG-03) imply buyers can be prevented from purchasing without necessarily having their account blocked at the `UserStatus` level (e.g. for commercial reasons rather than security ones).

| Code | Name | Description |
|------|------|-------------|
| ENABLED | Enabled | Buyer can place orders normally. |
| RESTRICTED | Restricted | Buyer is temporarily prevented from purchasing. |

---

# ProductType

## Description

Represents whether a product is physical or digital, determining whether it requires inventory and shipping.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** DOMINIO 5 — "Tipo de Producto: Físico o Digital."

| Code | Name | Description |
|------|------|-------------|
| PHYSICAL | Physical | Requires inventory tracking and physical dispatch. |
| DIGITAL | Digital | Delivered immediately upon payment. |

---

# ProductStatus

## Description

Represents the publication state of a product in the catalog.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** DOMINIO 5 — "Estado: Publicado, Suspendido o Descontinuado."

| Code | Name | Description |
|------|------|-------------|
| PUBLISHED | Published | Visible in the public catalog. |
| SUSPENDED | Suspended | Temporarily hidden from the catalog. |
| DISCONTINUED | Discontinued | Permanently removed from active sale. |

---

# InventoryStatus

## Description

Represents the condition of a stock record, used to determine whether it is eligible for reservation.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** Sección 11 — "No se puede reservar inventario inexistente o marcado como 'Dañado'." `AVAILABLE` is inferred as the necessary counterpart to `DAMAGED` — the validation rule only makes sense as a binary distinction between stock that can and cannot be reserved.

| Code | Name | Description |
|------|------|-------------|
| AVAILABLE | Available | Stock can be normally reserved and sold. |
| DAMAGED | Damaged | Stock is marked as damaged and cannot be reserved. |

---

# InventoryMovementType

## Description

Represents the type of change applied to an inventory record.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** DOMINIO 6 — "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución."

| Code | Name | Description |
|------|------|-------------|
| INBOUND | Inbound | Stock entering the warehouse. |
| RESERVATION | Reservation | Stock reserved for a pending order. |
| SALE_OUTBOUND | Sale Outbound | Stock leaving the warehouse due to a sale. |
| ADJUSTMENT | Adjustment | Manual correction of stock quantity. |
| RETURN | Return | Stock re-entering due to a product return. |

---

# OrderStatus

## Description

Represents the current stage of an order's lifecycle, including its initial "Cart" stage.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** DOMINIO 7 — "Ciclo de Estados del Pedido."

| Code | Name | Description |
|------|------|-------------|
| CART | Cart | Provisional selection of products. |
| PENDING_PAYMENT | Pending Payment | Awaiting financial confirmation. |
| PAID | Paid | Payment confirmed; fulfillment process begins. |
| DISPATCHED | Dispatched | Order has physically left the warehouse. |
| DELIVERED | Delivered / Finished | Delivery confirmed; the order is closed and can no longer be modified. |

---

# ShipmentStatus

## Description

Represents the current execution stage of a shipment.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** OBJ-10; Sección 4.1 name "Envíos" as a process; Sección 6.1 step 7 describes packing, dispatch, and transport as sequential sub-steps. The values below map directly onto that sequence, mirroring the granularity `OrderStatus` uses for its own dispatch/delivery stages.

| Code | Name | Description |
|------|------|-------------|
| PENDING | Pending | Shipment created, not yet dispatched (packing stage). |
| IN_TRANSIT | In Transit | Shipment dispatched, en route to the buyer. |
| DELIVERED | Delivered | Shipment confirmed as delivered — triggers `Order.orderStatus = DELIVERED`. |

---

# ReturnStatus

## Description

Represents the current stage of a return request.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** OBJ-11 names "devoluciones" as a process requiring administration. The values below follow the standard request→decision→closure pattern already used elsewhere in the specification for approval-driven flows (e.g. the Loan lifecycle in the banking reference, and this system's own Order lifecycle).

| Code | Name | Description |
|------|------|-------------|
| REQUESTED | Requested | Return request created by the buyer. |
| APPROVED | Approved | Return accepted, eligible for refund. |
| REJECTED | Rejected | Return request denied. |
| COMPLETED | Completed | Returned product processed and closed. |

---

# RefundStatus

## Description

Represents the current stage of a monetary refund.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** OBJ-11 names "reembolsos" as a process administered separately from returns, per the Matriz de Responsabilidades (Admin executes refunds).

| Code | Name | Description |
|------|------|-------------|
| PENDING | Pending | Refund approved but not yet processed. |
| PROCESSED | Processed | Funds returned to the buyer. |
| REJECTED | Rejected | Refund request denied. |

---

# OperationType

## Description

Represents the type of business operation recorded for audit purposes, referenced by `AuditLog`.

## Inherits From

DomainCatalog

## Allowed Values

**Source:** Derived directly from the core business events already identified elsewhere in this domain model (seller onboarding, catalog publication, inventory movements, order/payment lifecycle, shipment dispatch, and the return/refund flow), so that `AuditLog` can cover the full traceability scope implied by OBJ-12.

| Code | Name | Description |
|------|------|-------------|
| SELLER_REGISTRATION | Seller Registration | A seller was onboarded by an administrator. |
| PRODUCT_PUBLICATION | Product Publication | A product was published to the catalog. |
| INVENTORY_MOVEMENT | Inventory Movement | An inventory movement was recorded. |
| ORDER_PLACED | Order Placed | An order moved from Cart to Pending Payment. |
| ORDER_PAID | Order Paid | Payment for an order was confirmed. |
| SHIPMENT_DISPATCHED | Shipment Dispatched | A shipment left a warehouse. |
| RETURN_REQUESTED | Return Requested | A buyer requested a return. |
| REFUND_PROCESSED | Refund Processed | A refund was processed by an administrator. |

---

# Primitive Enumerations

The following concepts would be simple enumerations because they represent fixed technical values without business behavior.

None were identified from the NexusMarket specification. Unlike the banking example, the source document gives no basis for technical-only concepts such as notification channels or severity levels, so none were added here — every catalog above ties back to an explicit business concept in the specification.

---

# Design Notes

- All business catalogs inherit from **DomainCatalog**.
- Value Objects are immutable.
- Equality is determined by their values rather than object identity.
- Business entities reference Value Objects instead of primitive strings.
- No Primitive Enumerations were identified; see the section above.
- Every catalog whose values are not fully enumerated in the specification states its inference rationale directly in its own section, so the reasoning travels with the artifact rather than needing a separate review list.
- This approach improves maintainability, consistency, and alignment with Domain-Driven Design (DDD) principles while supporting future domain evolution.
