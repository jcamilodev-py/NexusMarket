# Domain Model

## Introduction

The Domain Model represents the core business entities of the NexusMarket marketplace platform. These entities encapsulate the business rules, data, and relationships described in the *Especificación Funcional del Negocio - NexusMarket*.

Where the specification names a business concept but does not enumerate its full attribute set, the missing attributes have been inferred from the context (business flow, objectives, and validation rules), the same way the banking reference.

---

# Domain Class Hierarchy

```text
User (Abstract)
├── Buyer
├── Seller
├── LogisticsOperator
├── Administrator
└── Supervisor

Warehouse (Abstract)
├── MarketplaceWarehouse
└── SellerWarehouse

Product (Abstract)
├── PhysicalProduct
└── DigitalProduct

ProductVariant

Inventory
InventoryMovement

Cart
CartItem

Order
OrderItem

Invoice
Shipment
ReturnRequest
Refund

AuditLog
```

---

# Entities

---

# User (Abstract)

## Description

Represents any participant authorized to interact with the NexusMarket platform. This abstract class centralizes the identification and access information shared by every role in the system (Buyer, Seller, Logistics Operator, Administrator, Supervisor).

Each participant plays exactly one role in the system (RG-02), and this class cannot be instantiated directly.

**Design decision — single-level hierarchy vs. the banking reference:** the banking example splits identity into three levels (`Person` → `Customer` → `User`) because a business customer there can have several distinct operational users referencing the same company. NexusMarket has no equivalent case in its specification — each seller, buyer, or employee corresponds to exactly one account — so a single `User` hierarchy with role-specific subclasses is sufficient and avoids introducing a distinction the business rules don't require.

**Source:** DOMINIO 1. Administración de Usuarios.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the user. |
| fullName | String | Official full name of the user. |
| email | String | Primary means of access and communication. Unique across the platform. |
| role | SystemRole | Defines the user's responsibilities and permissions. Unique per user. |
| status | UserStatus | Operational condition of the user (e.g. Active, Blocked). |

---

# Buyer

## Description

Represents a registered customer who purchases products published on the marketplace.

A buyer never manages information belonging to other buyers or to inventory (explicit restriction in the specification).

**Source:** DOMINIO 2. Gestión de Compradores.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| primaryAddress | String | Usual delivery location. |
| additionalAddresses | List\<String\> | Secondary delivery locations. Optional. |
| commercialStatus | BuyerCommercialStatus | Condition of the buyer for placing purchases. |

---

# Seller

## Description

Represents a vendor responsible for registering and managing their own products. Sellers cannot self-register; they are onboarded exclusively by an Administrator, together with their first warehouse.

Modeled with its own commercial identity, by analogy with `BusinessCustomer` in the banking reference example: a marketplace seller sells to third parties and must be identifiable as a fiscal/commercial entity (for invoicing, tax purposes, and public storefront display), even though the specification does not spell out this attribute set explicitly — it only states the onboarding rule.

**Source:** DOMINIO 3. Gestión de Vendedores (onboarding rule); attributes inferred by analogy with the banking reference's `BusinessCustomer`.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| legalBusinessName | String | Legal/registered name of the seller's business. Inferred, by analogy with `BusinessCustomer.legalName` in the banking reference. |
| taxId | String | Tax identification number (NIT/RUT or equivalent). Inferred: required for invoicing (`Invoice`) and to operate legally as a commercial entity. |
| tradeName | String | Public-facing storefront name shown to buyers in the catalog. Optional; may differ from the legal name. |

---

# LogisticsOperator

## Description

Represents the participant responsible for the physical operation of warehouses and dispatches.

**Source:** Sección 5. Participantes del Negocio.

## Attributes

No additional attributes beyond those inherited from `User`.

---

# Administrator

## Description

Represents the participant responsible for administering sellers and warehouses, including seller onboarding.

**Source:** Sección 5. Participantes del Negocio.

## Attributes

No additional attributes beyond those inherited from `User`.

---

# Supervisor

## Description

Represents a read-only/oversight profile for operational follow-up.

**Source:** Sección 5. Participantes del Negocio.

## Attributes

No additional attributes beyond those inherited from `User`.

---

# Warehouse (Abstract)

## Description

Represents a physical storage location used to manage inventory. The specification distinguishes between warehouses owned by the marketplace and warehouses owned by sellers.

This class cannot be instantiated directly.

**Source:** DOMINIO 4. Gestión de Bodegas.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the warehouse. |
| address | String | Physical location of the warehouse. Inferred: a storage location is meaningless without a physical location, and Sección 11 speaks of inventory being tied to "una bodega específica", implying warehouses are individually addressable places. |

---

# MarketplaceWarehouse

## Description

Represents a warehouse owned and operated directly by NexusMarket.

**Source:** DOMINIO 4 — "Clasificación: bodegas del Marketplace."

## Attributes

No additional attributes beyond those inherited from `Warehouse`.

---

# SellerWarehouse

## Description

Represents a warehouse owned by a specific seller. Every seller is registered together with their first warehouse (onboarding flow, step 1).

**Source:** DOMINIO 4 — "Clasificación: bodegas de Vendedores"; Sección 6.1 — "El Administrador registra al vendedor y su primera bodega."

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| owner | Seller | Seller who owns this warehouse. |

---

# Product (Abstract)

## Description

Represents a good offered for sale on the marketplace catalog. The catalog differentiates between physical products (require inventory and dispatch) and digital products (immediate delivery upon payment).

This class cannot be instantiated directly.

**Source:** DOMINIO 5. Gestión del Catálogo.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the product. |
| name | String | Commercial name of the product. Inferred: a catalog cannot be browsed or published without a display name. |
| description | String | Descriptive text of the product. Inferred: standard catalog attribute, implied by "Gestión del Catálogo" as a browsable public listing (Sección 6.1 step 4 — "Los productos se hacen visibles en el catálogo público"). |
| seller | Seller | Seller who registered and owns the product. Directly implied by the Matriz de Responsabilidades ("Registro Productos → Vendedor"). |
| price | BigDecimal | Sale price of the product. Inferred: a commercial catalog and a checkout/payment flow (Sección 6.1 steps 5-6) cannot function without a price per product. |
| variants | List\<ProductVariant\> | Differences in color, size, model, etc. |
| productStatus | ProductStatus | Published, Suspended or Discontinued. |

---

# PhysicalProduct

## Description

Represents a tangible product that requires inventory tracking and physical dispatch through a warehouse.

**Source:** DOMINIO 5 — "productos físicos (requieren inventario y despacho)."

## Attributes

No additional attributes. The distinction from `DigitalProduct` is behavioral: only `PhysicalProduct` participates in `Inventory` and `Shipment`.

---

# DigitalProduct

## Description

Represents an intangible product delivered immediately upon payment confirmation, without warehouse or shipment involvement.

**Source:** DOMINIO 5 — "productos digitales (entrega inmediata tras pago)."

## Attributes

No additional attributes. The specification only states the delivery timing rule (immediate, post-payment), not a delivery mechanism, so none is modeled here to keep the entity strictly aligned with what is stated.

---

# ProductVariant

## Description

Represents a specific variation of a product, such as color, size, or model.

**Design decision — generic key/value vs. fixed fields:** fixed fields (e.g. `color`, `size`) would need a schema change every time a new variant dimension appears (e.g. "material", "capacity"), while a generic `attributeName`/`attributeValue` pair covers any variant type the specification's "etc." implies, without altering the model. This favors long-term flexibility over short-term specificity.

**Source:** DOMINIO 5 — "Variantes: Diferencias de color, talla, modelo, etc."

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| attributeName | String | Name of the varying characteristic (e.g. "Color", "Talla"). Inferred: a "list of variants" needs a key/value structure to be usable; this is the minimal shape that represents "color, talla, modelo, etc." generically. |
| attributeValue | String | Value of that characteristic (e.g. "Rojo", "M"). |

---

# Inventory

## Description

Represents the stock of a specific product within a specific warehouse. Inventory is distributed and must always be linked to exactly one product and one warehouse. Negative stock is never permitted.

**Source:** DOMINIO 6. Gestión del Inventario.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the inventory record. |
| product | Product | Product this inventory record refers to. Mandatory. |
| warehouse | Warehouse | Warehouse this inventory record belongs to. Mandatory. |
| availableQuantity | Integer | Quantity currently available for sale. Must never be negative. |
| reservedQuantity | Integer | Quantity reserved by pending orders. Inferred: the "Reserva" movement type (Sección 6, Movimientos) only makes sense if reserved stock is tracked separately from available stock — otherwise a reservation could not be distinguished from an outright sale. |
| inventoryStatus | InventoryStatus | Condition of the stock (e.g. Available, Damaged). |

---

# InventoryMovement

## Description

Represents a single change applied to an inventory record, providing traceability for stock changes.

**Source:** DOMINIO 6 — "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución."

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the movement. |
| inventory | Inventory | Inventory record affected by this movement. |
| movementType | InventoryMovementType | Type of movement (Inbound, Reservation, Sale Outbound, Adjustment, Return). |
| quantity | Integer | Quantity involved in the movement. |
| movementDate | LocalDateTime | Date and time the movement occurred. Inferred: any traceable business event needs a timestamp, consistent with how the specification treats timing elsewhere (e.g. the order lifecycle). |
| performedBy | User | User who triggered the movement. Inferred from the Matriz de Responsabilidades, where inventory administration is explicitly shared between Seller and Operador Logístico — the movement needs to record which of them acted. |

---

# Cart

## Description

Represents a buyer's provisional, editable selection of products before checkout. A cart carries no commercial commitment and can be freely modified — unlike an `Order`, which is immutable once created.

Modeled as a separate entity from `Order`, since OBJ-07 ("Gestionar el carrito de compras") and OBJ-08 ("Controlar el ciclo completo de los pedidos") are listed as two distinct functional objectives in the specification, implying two distinct concepts rather than a single one. The cart is converted into an `Order` once the buyer confirms the purchase (Sección 6.1, step 5 → step 6), at which point the order lifecycle described in DOMINIO 7 begins.

**Source:** OBJ-07; Sección 6.1 step 5 ("El comprador selecciona productos mediante el carrito y confirma el pedido").

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the cart. Inferred. |
| buyer | Buyer | Buyer who owns the cart. |
| cartItems | List\<CartItem\> | Products currently selected in the cart. |
| creationDate | LocalDateTime | Date and time the cart was created. Inferred. |

---

# CartItem

## Description

Represents a single product line within a cart, before checkout.

**Source:** Inferred, mirroring `OrderItem` — a cart cannot hold "selected products" without a line-item structure.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| product | Product | Product selected by the buyer. |
| quantity | Integer | Quantity of the product currently selected. |

---

# Order

## Description

Represents the buyer's formal commercial commitment, created once a `Cart` is confirmed at checkout. Its lifecycle is the central process of the system, covering OBJ-08.

An order that reaches the "Finalizado" status can no longer be modified (Validaciones Críticas, Sección 11).

**Source:** DOMINIO 7. Gestión de Pedidos; Sección 6.1 step 6 ("Se valida el pago y se inicia el flujo de preparación").

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the order. |
| buyer | Buyer | Buyer who owns the order. |
| orderItems | List\<OrderItem\> | Products confirmed within the order, copied from the originating cart at checkout. |
| orderStatus | OrderStatus | Pending Payment, Paid, Dispatched, or Delivered/Finished. |
| creationDate | LocalDateTime | Date and time the order was created (i.e. when the cart was confirmed). |
| totalAmount | BigDecimal | Total value of the order. |

---

# OrderItem

## Description

Represents a single confirmed product line within an order, with the quantity and price locked in at the moment of purchase.

**Source:** DOMINIO 7; Sección 6.1 step 5.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| product | Product | Product selected by the buyer. |
| quantity | Integer | Quantity of the product requested. |
| unitPrice | BigDecimal | Product price at the moment the order was confirmed. Kept independent from `Product.price` so that later price changes do not alter historical orders. |
| subtotal | BigDecimal | `quantity * unitPrice`. |

---

# Invoice

## Description

Represents the commercial billing information associated with a completed sale.

**Source:** OBJ-09; Sección 4.1 — "Facturación: Información comercial asociada a las ventas."

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the invoice. |
| order | Order | Order this invoice was generated for. Every invoice traces back to exactly one commercial transaction. |
| buyer | Buyer | Buyer being billed. |
| issueDate | LocalDateTime | Date and time the invoice was issued — issued once the order reaches "Pagado" (Sección 6.1 step 6). |
| totalAmount | BigDecimal | Total billed amount, taken from the order total. |

---

# Shipment

## Description

Represents the logistics process of packing, dispatching, and transporting a physical order to the buyer.

**Source:** OBJ-10; Sección 4.1 — "Envíos: Procesos logísticos para productos físicos"; Sección 6.1 step 7 ("Se realiza el empaque, despacho y transporte del pedido").

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the shipment. |
| order | Order | Order being shipped. |
| originWarehouse | Warehouse | Warehouse the shipment was dispatched from. |
| logisticsOperator | LogisticsOperator | Operator responsible for the dispatch — directly implied by the Matriz de Responsabilidades. |
| shipmentStatus | ShipmentStatus | Current status of the shipment. |
| dispatchDate | LocalDateTime | Date and time the shipment left the warehouse. |
| deliveryDate | LocalDateTime | Date and time the shipment was delivered — the trigger for `Order.orderStatus = DELIVERED` (Sección 6.1 step 8). |

---

# ReturnRequest

## Description

Represents a buyer's request to return one or more products from an order.

**Source:** OBJ-11; Matriz de Responsabilidades (Comprador solicita reembolsos, i.e. initiates the return/refund flow).

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the return request. |
| order | Order | Order the return refers to. |
| requestedBy | Buyer | Buyer who requested the return — matches the Matriz de Responsabilidades, where only the Comprador row is checked for this process. |
| reason | String | Reason given for the return. |
| requestDate | LocalDateTime | Date and time the request was created. |
| returnStatus | ReturnStatus | Current status of the return request. |

---

# Refund

## Description

Represents the monetary reimbursement processed as a result of an approved return, modeled as a separate entity from `ReturnRequest` because the specification's own objectives list separates "devoluciones" (OBJ-11 product return) from "reembolsos" (money movement), and its Matriz de Responsabilidades assigns the refund step specifically to the Administrator, not the Buyer.

**Source:** OBJ-11; Matriz de Responsabilidades (Gestión Reembolsos → Comprador solicita, Admin ejecuta).

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the refund. |
| returnRequest | ReturnRequest | Return request that originated this refund. |
| amount | BigDecimal | Amount reimbursed to the buyer. |
| refundStatus | RefundStatus | Current status of the refund. |
| processedBy | Administrator | Administrator who processed the refund, per the Matriz de Responsabilidades. |
| processDate | LocalDateTime | Date and time the refund was processed. |

---

# AuditLog

## Description

Represents an immutable audit trail of significant business events, persisted in a NoSQL store, mirroring the pattern used in the banking reference example.

Modeled to satisfy OBJ-12 ("Consolidar información administrativa para consulta") together with the specification's opening statement that the system must guarantee "trazabilidad y coordinación entre todos los participantes" (Sección 1). Since these two requirements describe cross-cutting administrative traceability rather than a single business transaction, a dedicated audit record — separate from any individual `Order`, `Inventory`, or `Shipment` entity — is the natural way to satisfy them, consistent with how the same need was solved in the banking domain.

**Source:** OBJ-12; Sección 1 (Introducción y Contexto).

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| auditId | String | Uniquely identifies the audit record. |
| operationType | OperationType | Type of business event recorded. |
| operationDate | LocalDateTime | Timestamp when the event occurred. |
| performedBy | User | User responsible for the operation. |
| userRole | SystemRole | Role of the user at the time of execution. |
| affectedEntity | String | Reference/description of the entity affected by the operation. |
| details | Map\<String, Object\> | Flexible document with operation-specific information. |