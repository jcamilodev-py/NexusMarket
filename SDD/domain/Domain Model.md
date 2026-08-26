# Domain Model

## Introduction

The Domain Model represents the core business entities of the NexusMarket marketplace platform. These entities encapsulate the business rules, data, relationships, and lifecycle concepts described in the *Especificación Funcional del Negocio - NexusMarket*.

The model follows Object-Oriented Design and Domain-Driven Design (DDD) principles. Inheritance is used to represent genuine domain specialization, while explicit object relationships are preferred over generic identifier fields.

The model distinguishes between:

* **Users**, which represent the people authorized to interact with the platform and the single role each one plays within it.
* **Warehouses**, which represent the physical locations where stock is held.
* **Products and Product Variants**, which represent the goods offered in the catalog and the specific sellable units derived from them.
* **Inventory**, which represents the distributed stock of each sellable unit in each warehouse.
* **Commercial documents**, which represent the buyer's purchase journey: `Cart`, `Order`, `Payment`, and `Invoice`.
* **Fulfilment and after-sales entities**, which represent `Shipment`, `ReturnRequest`, and `Refund`.
* **Operations**, which represent significant business actions performed within the marketplace.
* **Audit Logs**, which provide an immutable historical record of operations.

A business entity may generate multiple operations throughout its lifecycle. Every significant business operation must be recorded in the audit trail.

Where the specification names a business concept but does not enumerate its full attribute set, the missing attributes have been inferred from the business context — the operating flow, the functional objectives, and the critical validations — in the same way as the banking reference model. Every entity states its **Source** in the specification, and every inferred attribute states its justification inline, so that traceability travels inside the document rather than in a separate review list.

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
Payment
Invoice

Shipment

ReturnRequest
ReturnItem
Refund

Operation

AuditLog
```

---

# Domain Relationships

```text
User
   ├── Buyer
   │      ├── owns (exactly one active) ──> Cart
   │      ├── places ───────────────────────> Order
   │      ├── is billed in ─────────────────> Invoice
   │      └── requests ─────────────────────> ReturnRequest
   │
   ├── Seller
   │      ├── owns (at least one) ──────────> SellerWarehouse
   │      └── registers ────────────────────> Product
   │
   ├── LogisticsOperator
   │      └── is responsible for ───────────> Shipment
   │
   ├── Administrator
   │      ├── onboards ─────────────────────> Seller
   │      ├── administers ──────────────────> Warehouse
   │      └── processes ────────────────────> Refund
   │
   └── Supervisor
          └── consults ─────────────────────> Operation / AuditLog

Warehouse
   ├── MarketplaceWarehouse
   └── SellerWarehouse
          └── owner ─────────────────────────> Seller

Product
   ├── PhysicalProduct
   ├── DigitalProduct
   ├── seller ───────────────────────────────> Seller
   └── has (at least one) ───────────────────> ProductVariant
                                                    │
                                                    └── stocked as ──> Inventory
                                                                          ├── warehouse ──> Warehouse
                                                                          └── changed by ─> InventoryMovement
                                                                                                 └── performedBy ──> User

Cart
   ├── buyer ────────────────────────────────> Buyer
   ├── contains ─────────────────────────────> CartItem
   │                                                └── variant ──> ProductVariant
   └── confirmed into ───────────────────────> Order

Order
   ├── buyer ────────────────────────────────> Buyer
   ├── contains ─────────────────────────────> OrderItem
   │                                                └── variant ──> ProductVariant
   ├── settled by ───────────────────────────> Payment
   ├── billed by ────────────────────────────> Invoice
   ├── fulfilled by ─────────────────────────> Shipment
   │                                                ├── originWarehouse ───> Warehouse
   │                                                ├── logisticsOperator ─> LogisticsOperator
   │                                                └── items ─────────────> OrderItem
   └── disputed by ──────────────────────────> ReturnRequest
                                                     ├── contains ───> ReturnItem
                                                     │                     └── orderItem ──> OrderItem
                                                     └── settled by ─> Refund
                                                                           └── processedBy ──> Administrator

Any business entity
   │
   │ significant business action
   ▼
Operation
   ├── performedBy ──────────────────────────> User
   └── recorded in ──────────────────────────> AuditLog
```

---

# Entities

---

# User (Abstract)

## Description

Represents any participant authorized to interact with the NexusMarket platform.

This abstract class centralizes the identification, contact, and access information shared by every role in the system: Buyer, Seller, Logistics Operator, Administrator, and Supervisor.

Each participant plays exactly one role within the system, and that role determines the responsibilities and business capabilities associated with the participant.

**Design decision — single-level hierarchy vs. the banking reference:** the banking reference splits identity into three levels (`Person` → `Customer` → `User`) because a business customer there may have several distinct operational users referencing the same company. NexusMarket has no equivalent case in its specification — each buyer, seller, or employee corresponds to exactly one account — so a single `User` hierarchy with role-specific subclasses is sufficient and avoids introducing a distinction the business rules do not require.

**Design decision — identity document vs. internal identifier:** DOMINIO 1 lists a single ambiguous "Identificador", while Sección 11 requires that **both** the identity document and the email address be unique across the platform. One field cannot carry both concerns, so the model separates them explicitly, mirroring the banking reference (`Person.identification` for the national identity document, `User.userId` for the internal system identifier).

This class cannot be instantiated directly.

**Source:** DOMINIO 1. Administración de Usuarios; Sección 5. Participantes del Negocio; Sección 11. Validaciones Críticas; RG-01, RG-02, RG-03.

## Attributes

| Attribute            | Type       | Description                                                                                                                                                                                                                                                               |
| -------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| userId               | String     | Internal unique identifier of the user within the platform. Inferred: separated from `identificationNumber` because the identity document is a business datum belonging to the person, while this identifier belongs to the system.                                        |
| identificationNumber | String     | National identity document number of the participant. Unique across the platform. Inferred from Sección 11 ("El documento de identidad y correo electrónico deben ser únicos en la plataforma"), which requires this datum even though DOMINIO 1 does not list it separately. |
| fullName             | String     | Official full name of the user.                                                                                                                                                                                                                                           |
| email                | String     | Primary means of access and communication. Unique across the platform.                                                                                                                                                                                                    |
| role                 | SystemRole | Business role that defines the user's responsibilities and permissions. Exactly one per user.                                                                                                                                                                              |
| status               | UserStatus | Current operational condition of the user, such as Active or Blocked.                                                                                                                                                                                                     |

## Relationships

* A `User` is specialized as exactly one of `Buyer`, `Seller`, `LogisticsOperator`, `Administrator`, or `Supervisor`.
* A `User` may perform zero or more `Operation` instances.
* Each `Operation` records the `User` responsible for it, and each `AuditLog` additionally records the `SystemRole` held at the time of execution.
* The `role` belongs to `User` because it represents the participant's meaning and responsibilities within the marketplace, and it is what determines which specialization applies.

## Business Rules

```text
Every operation must be executed by an authenticated user.            (RG-01)
Each user holds exactly one role within the system.                   (RG-02)
No participant may administer information outside their own role.     (RG-03)
identificationNumber must be unique across the platform.              (Sección 11)
email must be unique across the platform.                             (Sección 11)
```

## Examples of Generated Operations

* `USER_REGISTRATION`
* `USER_STATUS_CHANGE`

---

# Buyer

## Description

Represents a registered customer who purchases products published on the marketplace.

A buyer holds the delivery addresses used to fulfil their orders and a commercial status that determines whether they may currently place purchases.

A buyer never manages information belonging to other buyers or to inventory, which is an explicit restriction of the specification.

**Source:** DOMINIO 2. Gestión de Compradores; OBJ-03; Matriz de Responsabilidades.

## Inherits From

`User`

## Attributes

| Attribute           | Type                  | Description                                                                                                                                                                                                                                     |
| ------------------- | --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| primaryAddress      | String                | Usual delivery location of the buyer.                                                                                                                                                                                                           |
| additionalAddresses | List\<String\>        | Secondary delivery locations. Optional; empty by default.                                                                                                                                                                                       |
| commercialStatus    | BuyerCommercialStatus | Condition of the buyer for placing purchases.                                                                                                                                                                                                   |
| activeCart          | Cart?                 | The buyer's single open cart. Inferred: OBJ-07 treats the cart as an administered business object, and a buyer must be able to return to a selection already in progress, which requires a stable reference to it.                               |
| orders              | List\<Order\>         | Orders placed by the buyer. Empty by default. Inferred, mirroring the banking reference: not populated by default, loaded on demand by the corresponding consultation service.                                                                    |

## Relationships

* A `Buyer` owns exactly one active `Cart` at any given time.
* A `Buyer` places zero or more `Order` instances.
* A `Buyer` is the billed party of zero or more `Invoice` instances, always through an `Order`.
* A `Buyer` requests zero or more `ReturnRequest` instances.
* A `Buyer` is the recipient of zero or more `Refund` instances, always through a `ReturnRequest`.
* `orders` is not populated by default; it is loaded on demand.

## Business Rules

```text
A buyer has exactly one active Cart at any given time.
A buyer whose commercialStatus is not ENABLED cannot confirm an Order.
A buyer may only access the orders, carts, invoices, returns, and
refunds that belong to them.                                          (DOMINIO 2, RG-03)
```

## Examples of Generated Operations

* `BUYER_REGISTRATION`
* `ORDER_PLACEMENT`
* `RETURN_REQUEST_CREATION`

---

# Seller

## Description

Represents a vendor responsible for registering and managing their own products.

Sellers cannot self-register; they are onboarded exclusively by an `Administrator`, together with their first warehouse.

Modeled with its own commercial identity, by analogy with `BusinessCustomer` in the banking reference: a marketplace seller sells to third parties and must be identifiable as a fiscal and commercial entity — for invoicing, for tax purposes, and for public storefront display — even though the specification does not spell out this attribute set explicitly and states only the onboarding rule.

**Source:** DOMINIO 3. Gestión de Vendedores; OBJ-02; Sección 6.1 step 1; Matriz de Responsabilidades ("Registro Vendedores → Admin", "Registro Productos → Vendedor"). Commercial identity attributes inferred by analogy with the banking reference's `BusinessCustomer`.

## Inherits From

`User`

## Attributes

| Attribute         | Type                    | Description                                                                                                                                                             |
| ----------------- | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| legalBusinessName | String                  | Legal or registered name of the seller's business. Inferred, by analogy with `BusinessCustomer.legalName` in the banking reference.                                      |
| taxId             | String                  | Tax identification number (NIT/RUT or equivalent). Inferred: required to issue an `Invoice` and to operate legally as a commercial entity.                               |
| tradeName         | String                  | Public-facing storefront name shown to buyers in the catalog. Optional; may differ from the legal name.                                                                  |
| warehouses        | List\<SellerWarehouse\> | Warehouses owned by the seller. Contains at least one element from the moment of onboarding.                                                                             |
| products          | List\<Product\>         | Products registered by the seller. Empty by default. Inferred, mirroring the banking reference: loaded on demand by the corresponding consultation service.              |

## Relationships

* A `Seller` owns one or more `SellerWarehouse` instances.
* A `Seller` registers zero or more `Product` instances.
* A `Seller` is onboarded by an `Administrator` and cannot be created by any other role.
* A `Seller` participates indirectly in `Order` instances, through the products sold.
* A `Seller` may register `InventoryMovement` instances over the inventory of their own products.

## Business Rules

```text
A Seller is always created by an Administrator; self-registration
is not permitted.                                                     (DOMINIO 3)
Every Seller owns at least one SellerWarehouse, created during
onboarding together with the seller record.                           (Sección 6.1, step 1)
A Seller may only manage their own products and their own inventory.  (RG-03)
```

## Examples of Generated Operations

* `SELLER_REGISTRATION`
* `PRODUCT_REGISTRATION`
* `PRODUCT_PUBLICATION`
* `INVENTORY_ADJUSTMENT`

---

# LogisticsOperator

## Description

Represents the participant responsible for the physical operation of warehouses and dispatches.

The logistics operator performs the packing, dispatch, and transport activities of the fulfilment flow, and shares inventory administration responsibilities with the seller.

**Source:** Sección 5. Participantes del Negocio; OBJ-10; Sección 6.1 step 7; Matriz de Responsabilidades ("Administración Inventario", "Gestión de Pedidos").

## Inherits From

`User`

## Attributes

No additional attributes beyond those inherited from `User`.

## Relationships

* A `LogisticsOperator` is responsible for zero or more `Shipment` instances.
* A `LogisticsOperator` may register zero or more `InventoryMovement` instances.
* A `LogisticsOperator` operates over `Warehouse` instances but does not own them.

## Examples of Generated Operations

* `SHIPMENT_DISPATCH`
* `SHIPMENT_DELIVERY`
* `INVENTORY_INBOUND`
* `INVENTORY_ADJUSTMENT`

---

# Administrator

## Description

Represents the participant responsible for administering sellers and warehouses, including seller onboarding, and for executing refunds.

**Source:** Sección 5. Participantes del Negocio; DOMINIO 3; DOMINIO 4; Matriz de Responsabilidades ("Registro Vendedores → Admin", "Gestión Reembolsos → Admin").

## Inherits From

`User`

## Attributes

No additional attributes beyond those inherited from `User`.

## Relationships

* An `Administrator` onboards zero or more `Seller` instances.
* An `Administrator` administers zero or more `Warehouse` instances.
* An `Administrator` processes zero or more `Refund` instances.

## Business Rules

```text
Only an Administrator may register a Seller.                          (DOMINIO 3)
Only an Administrator may process a Refund.                           (Matriz de Responsabilidades)
```

## Examples of Generated Operations

* `SELLER_REGISTRATION`
* `WAREHOUSE_REGISTRATION`
* `REFUND_PROCESSING`
* `USER_STATUS_CHANGE`

---

# Supervisor

## Description

Represents a consultation and operational follow-up profile.

The supervisor observes the operation but does not modify business information, which makes `Operation` and `AuditLog` records their primary working material.

**Source:** Sección 5. Participantes del Negocio ("Perfil de consulta y seguimiento operativo"); OBJ-12.

## Inherits From

`User`

## Attributes

No additional attributes beyond those inherited from `User`.

## Relationships

* A `Supervisor` consults `Operation` and `AuditLog` records.
* A `Supervisor` does not own, modify, or create commercial entities.

## Business Rules

```text
A Supervisor holds read-only access over the operational information
of the marketplace.                                                   (Sección 5, RG-03)
```

---

# Warehouse (Abstract)

## Description

Represents a physical storage location used to manage inventory.

The specification distinguishes between warehouses owned by the marketplace and warehouses owned by sellers. Inventory is always tied to one specific warehouse, which makes warehouses individually identifiable and addressable places.

This class cannot be instantiated directly.

**Source:** DOMINIO 4. Gestión de Bodegas; OBJ-04; DOMINIO 6 ("vinculado obligatoriamente a un producto y una bodega específica").

## Attributes

| Attribute  | Type   | Description                                                                                                                                                                                                                                          |
| ---------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identifier | String | Uniquely identifies the warehouse.                                                                                                                                                                                                                   |
| address    | String | Physical location of the warehouse. Inferred: a storage location is meaningless without a physical location, and DOMINIO 6 requires inventory to be tied to "una bodega específica", which implies warehouses are individually addressable places.    |

## Relationships

* A `Warehouse` holds zero or more `Inventory` records.
* A `Warehouse` may be the origin of zero or more `Shipment` instances.
* A `Warehouse` is administered by an `Administrator` and operated by a `LogisticsOperator`.

## Business Rules

```text
Inventory is always linked to exactly one Warehouse.                  (DOMINIO 6)
```

## Examples of Generated Operations

* `WAREHOUSE_REGISTRATION`

---

# MarketplaceWarehouse

## Description

Represents a warehouse owned and operated directly by NexusMarket.

**Source:** DOMINIO 4 — "Clasificación: Se distinguen bodegas del Marketplace y bodegas de Vendedores."

## Inherits From

`Warehouse`

## Attributes

No additional attributes beyond those inherited from `Warehouse`.

## Relationships

* A `MarketplaceWarehouse` has no owning `Seller`; it belongs to the marketplace itself.
* A `MarketplaceWarehouse` may hold `Inventory` records for variants belonging to any `Seller`.

---

# SellerWarehouse

## Description

Represents a warehouse owned by a specific seller.

Every seller is registered together with their first warehouse during the onboarding flow, which makes the ownership relationship mandatory in both directions.

**Source:** DOMINIO 4 — "Clasificación: bodegas de Vendedores"; Sección 6.1 step 1 — "El Administrador registra al vendedor y su primera bodega."

## Inherits From

`Warehouse`

## Attributes

| Attribute | Type   | Description                     |
| --------- | ------ | ------------------------------- |
| owner     | Seller | Seller who owns this warehouse. |

## Relationships

* A `SellerWarehouse` belongs to exactly one `Seller`.
* A `Seller` owns at least one `SellerWarehouse`.

## Business Rules

```text
A SellerWarehouse always has exactly one owning Seller.
Every Seller owns at least one SellerWarehouse.                       (Sección 6.1, step 1)
```

---

# Product (Abstract)

## Description

Represents a good offered for sale in the marketplace catalog.

The catalog differentiates between physical products, which require inventory and dispatch, and digital products, which are delivered immediately upon payment.

A product is a **catalog concept**: it carries the commercial description that buyers browse. It is not itself the sellable unit — that role belongs to `ProductVariant`, which is what inventory, carts, and orders actually reference.

This class cannot be instantiated directly.

**Source:** DOMINIO 5. Gestión del Catálogo; OBJ-05; Sección 6.1 steps 2 and 4; Matriz de Responsabilidades ("Registro Productos → Vendedor").

## Attributes

| Attribute     | Type                    | Description                                                                                                                                                                                                                                                        |
| ------------- | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identifier    | String                  | Uniquely identifies the product.                                                                                                                                                                                                                                   |
| name          | String                  | Commercial name of the product. Inferred: a catalog cannot be browsed or published without a display name.                                                                                                                                                          |
| description   | String                  | Descriptive text of the product. Inferred: standard catalog attribute, implied by DOMINIO 5 together with Sección 6.1 step 4 — "Los productos se hacen visibles en el catálogo público."                                                                            |
| productType   | ProductType             | Physical or Digital. Listed explicitly as a product attribute in DOMINIO 5, and kept as an attribute in addition to the subclass distinction so that the catalog can be filtered and validated without relying on the runtime type.                                  |
| seller        | Seller                  | Seller who registered and owns the product. Directly implied by the Matriz de Responsabilidades ("Registro Productos → Vendedor").                                                                                                                                  |
| price         | BigDecimal              | Sale price of the product. Inferred: a commercial catalog and a payment flow (Sección 6.1 steps 5–6) cannot function without a price.                                                                                                                               |
| currency      | Currency                | Currency in which `price` is expressed. Inferred: see the currency rationale in **Domain Design Rules**.                                                                                                                                                            |
| variants      | List\<ProductVariant\>  | Sellable variations of the product: colour, size, model, and so on. Contains at least one element.                                                                                                                                                                  |
| productStatus | ProductStatus           | Published, Suspended, or Discontinued.                                                                                                                                                                                                                             |

## Relationships

* A `Product` belongs to exactly one `Seller`.
* A `Product` has one or more `ProductVariant` instances.
* A `Product` is never referenced directly by `Inventory`, `CartItem`, or `OrderItem`; those entities reference a `ProductVariant`.
* A `Product` is specialized as either a `PhysicalProduct` or a `DigitalProduct`, and that specialization determines whether it participates in `Inventory` and `Shipment`.

## Business Rules

```text
Every Product has at least one ProductVariant. A product with no real
variation is registered with a single default variant, so that stock,
cart lines, and order lines always reference a variant.

All variants of a Product share the Product price. The specification
does not establish price differences between variants, so no separate
price is modeled at variant level.

Only the owning Seller may modify a Product.                          (RG-03)

Only a Product whose productStatus is PUBLISHED is visible in the
public catalog and may be added to a Cart.                            (DOMINIO 5, Sección 6.1 step 4)
```

## Examples of Generated Operations

* `PRODUCT_REGISTRATION`
* `PRODUCT_PUBLICATION`
* `PRODUCT_SUSPENSION`
* `PRODUCT_DISCONTINUATION`

---

# PhysicalProduct

## Description

Represents a tangible product that requires inventory tracking and physical dispatch through a warehouse.

**Source:** DOMINIO 5 — "productos físicos (requieren inventario y despacho)."

## Inherits From

`Product`

## Attributes

No additional attributes. The distinction from `DigitalProduct` is behavioural: only the variants of a `PhysicalProduct` participate in `Inventory`, and only its order lines participate in `Shipment`.

## Relationships

* The `ProductVariant` instances of a `PhysicalProduct` have `Inventory` records in one or more `Warehouse` instances.
* An `OrderItem` referencing a variant of a `PhysicalProduct` is fulfilled through a `Shipment`.

## Business Rules

```text
Every ProductVariant of a PhysicalProduct must have at least one
Inventory record before the product can be published.                 (DOMINIO 6, Sección 6.1 step 3)
```

---

# DigitalProduct

## Description

Represents an intangible product delivered immediately upon payment confirmation, without warehouse or shipment involvement.

**Source:** DOMINIO 5 — "productos digitales (entrega inmediata tras pago)."

## Inherits From

`Product`

## Attributes

No additional attributes. The specification states only the delivery timing rule — immediate, after payment — and not a delivery mechanism, so none is modeled here, in order to keep the entity strictly aligned with what is stated.

## Relationships

* A `DigitalProduct` and its variants never participate in `Inventory`.
* An `OrderItem` referencing a variant of a `DigitalProduct` never participates in a `Shipment`.

## Business Rules

```text
A DigitalProduct never generates Inventory records.                   (DOMINIO 5, DOMINIO 6)
A DigitalProduct never generates InventoryMovement records.
A DigitalProduct never participates in a Shipment.
An OrderItem referencing a DigitalProduct variant is considered
delivered as soon as the Order reaches PAID.                          (DOMINIO 5)
```

---

# ProductVariant

## Description

Represents a specific sellable variation of a product, such as a colour, a size, or a model.

The variant is the unit that the business actually sells and counts. Stock is held per variant, and cart and order lines record the exact variant selected by the buyer.

**Design decision — entity, not value object:** a variant has its own identity and its own commercial code (SKU), it is referenced independently by `Inventory`, `CartItem`, and `OrderItem`, and it is the level at which stock is counted. Modeling it as an attribute of `Product` would make it impossible to track stock for a concrete combination such as "red, size M", which the distributed inventory of DOMINIO 6 requires.

**Design decision — generic key/value vs. fixed fields:** fixed fields such as `color` or `size` would require a model change every time a new variant dimension appears, for example "material" or "capacity", whereas a generic `attributeName`/`attributeValue` pair covers any variant type implied by the specification's "etc." without altering the model.

**Source:** DOMINIO 5 — "Variantes: Diferencias de color, talla, modelo, etc."; DOMINIO 6 (distributed inventory tied to a specific product and warehouse).

## Attributes

| Attribute      | Type    | Description                                                                                                                                                                                                                          |
| -------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| variantId      | String  | Internal unique identifier of the variant. Inferred: required because the variant is referenced independently by `Inventory`, `CartItem`, and `OrderItem`.                                                                            |
| sku            | String  | Unique commercial code of the sellable unit (Stock Keeping Unit). Inferred: a distributed inventory operated by warehouse staff requires a stable business code to identify what is being counted, received, and dispatched.          |
| product        | Product | Product this variant belongs to.                                                                                                                                                                                                     |
| attributeName  | String  | Name of the varying characteristic, for example "Color" or "Talla". Inferred: a list of variants needs a key/value structure to be usable; this is the minimal shape that represents "color, talla, modelo, etc." generically.        |
| attributeValue | String  | Value of that characteristic, for example "Rojo" or "M".                                                                                                                                                                             |

## Relationships

* A `ProductVariant` belongs to exactly one `Product`.
* A `ProductVariant` has zero or more `Inventory` records, one per `Warehouse` that stocks it. Variants of a `DigitalProduct` have none.
* A `ProductVariant` may be referenced by zero or more `CartItem` instances.
* A `ProductVariant` may be referenced by zero or more `OrderItem` instances.

## Business Rules

```text
sku is unique across the platform.

A Product with no real variation is registered with a single default
variant, so that every Inventory, CartItem, and OrderItem always
references a variant and no optional product/variant path exists.

Variants of a DigitalProduct never have Inventory records.            (DOMINIO 5)
```

---

# Inventory

## Description

Represents the stock of a specific product variant within a specific warehouse.

Inventory is distributed: it must always be linked to exactly one variant and one warehouse. Negative stock is never permitted under any circumstance.

**Design decision — inventory per variant:** the specification requires inventory to be tied to "un producto y una bodega específica", but it also defines variants as differences of colour, size, and model. Stock counted at product level cannot answer how many units of "red, size M" remain in a given warehouse, so the association is made to `ProductVariant`, which is the specialization of the product concept that the business actually counts.

**Source:** DOMINIO 6. Gestión del Inventario; OBJ-06; Sección 11. Validaciones Críticas.

## Attributes

| Attribute         | Type            | Description                                                                                                                                                                                                                                          |
| ----------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| identifier        | String          | Uniquely identifies the inventory record.                                                                                                                                                                                                            |
| variant           | ProductVariant  | Product variant this inventory record refers to. Mandatory.                                                                                                                                                                                          |
| warehouse         | Warehouse       | Warehouse this inventory record belongs to. Mandatory.                                                                                                                                                                                               |
| availableQuantity | Integer         | Quantity currently available for sale. Must never be negative.                                                                                                                                                                                       |
| reservedQuantity  | Integer         | Quantity reserved by pending orders. Inferred: the "Reserva" movement type of DOMINIO 6 only makes sense if reserved stock is tracked separately from available stock; otherwise a reservation could not be distinguished from an outright sale.      |
| inventoryStatus   | InventoryStatus | Condition of the stock, such as Available or Damaged.                                                                                                                                                                                                |

## Relationships

* An `Inventory` record refers to exactly one `ProductVariant` and exactly one `Warehouse`.
* An `Inventory` record is modified only through `InventoryMovement` instances, which provide its traceability.
* An `Inventory` record supplies the stock consumed by `OrderItem` reservations and released by `ReturnItem` returns.

## Business Rules

```text
availableQuantity and reservedQuantity must never be negative.        (DOMINIO 6)

Stock that does not exist, or whose inventoryStatus is DAMAGED,
cannot be reserved.                                                   (Sección 11)

An Inventory record exists only for variants of a PhysicalProduct.    (DOMINIO 5)

Every change to availableQuantity or reservedQuantity must be
recorded as an InventoryMovement.                                     (DOMINIO 6)
```

## Examples of Generated Operations

* `INVENTORY_INBOUND`
* `INVENTORY_RESERVATION`
* `INVENTORY_SALE_OUTBOUND`
* `INVENTORY_ADJUSTMENT`
* `INVENTORY_RETURN`

---

# InventoryMovement

## Description

Represents a single change applied to an inventory record, providing traceability for every stock variation.

The movement is the historical record of what happened to the stock, while `Inventory` holds the resulting current quantities.

**Source:** DOMINIO 6 — "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución."

## Attributes

| Attribute    | Type                  | Description                                                                                                                                                                                                                       |
| ------------ | --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identifier   | String                | Uniquely identifies the movement.                                                                                                                                                                                                 |
| inventory    | Inventory             | Inventory record affected by this movement.                                                                                                                                                                                       |
| movementType | InventoryMovementType | Type of movement: Inbound, Reservation, Sale Outbound, Adjustment, or Return.                                                                                                                                                     |
| quantity     | Integer               | Quantity involved in the movement.                                                                                                                                                                                                |
| movementDate | LocalDateTime         | Date and time the movement occurred. Inferred: any traceable business event requires a timestamp, consistent with how the specification treats the timing of the order lifecycle.                                                  |
| performedBy  | User                  | User who triggered the movement. Inferred from the Matriz de Responsabilidades, where inventory administration is explicitly shared between Vendedor and Operador Logístico, so the movement must record which of them acted.      |

## Relationships

* An `InventoryMovement` affects exactly one `Inventory` record.
* An `InventoryMovement` is performed by exactly one `User`, who is either a `Seller` or a `LogisticsOperator`.
* An `InventoryMovement` of type `RESERVATION` or `SALE_OUTBOUND` originates from an `Order`.
* An `InventoryMovement` of type `RETURN` originates from an approved `ReturnRequest`.

## Business Rules

```text
An InventoryMovement is immutable once registered; corrections are
expressed as new movements of type ADJUSTMENT, never as edits.

A movement may never leave availableQuantity or reservedQuantity
negative.                                                             (DOMINIO 6)

Only a Seller (over their own products) or a LogisticsOperator may
register an InventoryMovement.                                        (Matriz de Responsabilidades, RG-03)
```

---

# Cart

## Description

Represents a buyer's provisional, editable selection of product variants before checkout.

A cart carries no commercial commitment and can be freely modified, unlike an `Order`, which represents a formal commitment.

Modeled as a separate entity from `Order` because OBJ-07 ("Gestionar el carrito de compras") and OBJ-08 ("Controlar el ciclo completo de los pedidos") are listed as two distinct functional objectives, which implies two distinct concepts rather than a single one. The cart is converted into an `Order` once the buyer confirms the purchase, at which point the order lifecycle described in DOMINIO 7 begins.

**Source:** OBJ-07; DOMINIO 7 (state "Carrito"); Sección 6.1 step 5 — "El comprador selecciona productos mediante el carrito y confirma el pedido."

## Attributes

| Attribute    | Type             | Description                                                                                                                                                              |
| ------------ | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| identifier   | String           | Uniquely identifies the cart. Inferred: required for the cart to be an addressable business object, as OBJ-07 implies.                                                   |
| buyer        | Buyer            | Buyer who owns the cart.                                                                                                                                                 |
| cartItems    | List\<CartItem\> | Product variants currently selected in the cart. May be empty.                                                                                                           |
| creationDate | LocalDateTime    | Date and time the cart was created. Inferred: consistent with the timestamping of every other traceable business object in this model.                                    |

## Relationships

* A `Cart` belongs to exactly one `Buyer`.
* A `Cart` contains zero or more `CartItem` instances.
* A `Cart` is confirmed into exactly one `Order` at checkout; from that moment it is no longer the buyer's active cart.

## Business Rules

```text
A Buyer has exactly one active Cart at any given time.

A Cart may only contain variants of Products whose productStatus
is PUBLISHED.                                                         (DOMINIO 5, Sección 6.1 step 4)

Confirming a Cart creates an Order and closes the Cart. The Cart is
never modified afterwards, so that the order it produced remains
reproducible.

A Cart carries no commercial commitment and reserves no inventory;
reservation occurs when the Order is created.                         (DOMINIO 6, DOMINIO 7)
```

## Examples of Generated Operations

* `CART_ITEM_ADDITION`
* `CART_ITEM_REMOVAL`
* `CART_CONFIRMATION`

---

# CartItem

## Description

Represents a single product variant line within a cart, before checkout.

Unlike `OrderItem`, a cart item does not freeze the price: the cart is a provisional selection and always reflects the current catalog price.

**Source:** OBJ-07; Sección 6.1 step 5. Line-item structure inferred, mirroring `OrderItem`: a cart cannot hold "productos seleccionados" without one.

## Attributes

| Attribute | Type           | Description                                                                                                                                       |
| --------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| cart      | Cart           | Cart this line belongs to.                                                                                                                        |
| variant   | ProductVariant | Product variant selected by the buyer.                                                                                                            |
| quantity  | Integer        | Quantity of the variant currently selected. Must be greater than zero.                                                                            |

## Relationships

* A `CartItem` belongs to exactly one `Cart`.
* A `CartItem` references exactly one `ProductVariant`.
* A `CartItem` becomes an `OrderItem` when the cart is confirmed.

## Business Rules

```text
quantity must be greater than zero.

A CartItem does not freeze the price; the applicable price is the
current Product price until the Order is confirmed.

The same ProductVariant appears at most once per Cart; adding it
again increases the quantity of the existing line.
```

---

# Order

## Description

Represents the buyer's formal commercial commitment, created once a `Cart` is confirmed at checkout. Its lifecycle is the central process of the system.

An order freezes the commercial conditions of the purchase — the variants selected, their quantities, their unit prices, and the delivery address — so that later changes to the catalog or to the buyer's profile never alter historical orders.

An order that reaches the finished state can no longer be modified under any circumstance.

**Source:** DOMINIO 7. Gestión de Pedidos; OBJ-08; Sección 6.1 steps 5–8; Sección 11. Validaciones Críticas.

## Attributes

| Attribute       | Type              | Description                                                                                                                                                                                                                                                                    |
| --------------- | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identifier      | String            | Uniquely identifies the order.                                                                                                                                                                                                                                                 |
| buyer           | Buyer             | Buyer who placed the order.                                                                                                                                                                                                                                                    |
| orderItems      | List\<OrderItem\> | Product variant lines confirmed within the order, copied from the originating cart at checkout. Contains at least one element.                                                                                                                                                  |
| shippingAddress | String            | Delivery address chosen for this order, copied at checkout. Inferred: DOMINIO 2 gives the buyer a primary address plus additional addresses, so the order must record which one was selected — for the same reason `OrderItem` freezes `unitPrice`.                              |
| orderStatus     | OrderStatus       | Current stage of the order lifecycle: Pending Payment, Paid, Dispatched, Delivered/Finished, or Cancelled. An order is created directly in `PENDING_PAYMENT`; the `CART` value of the catalog corresponds to the stage represented by the `Cart` entity and is never held by an `Order`.                                                                                                                                                                                 |
| creationDate    | LocalDateTime     | Date and time the order was created, that is, when the cart was confirmed.                                                                                                                                                                                                     |
| totalAmount     | BigDecimal        | Total value of the order, equal to the sum of the `subtotal` of its `orderItems`.                                                                                                                                                                                               |
| currency        | Currency          | Currency in which `totalAmount` is expressed. Inferred: see the currency rationale in **Domain Design Rules**.                                                                                                                                                                  |
| payments        | List\<Payment\>   | Payment attempts registered against the order. Contains at least one element from the moment payment is first attempted.                                                                                                                                                        |
| invoice         | Invoice?          | Commercial document issued for the order once it is paid. Absent while the order has not been paid.                                                                                                                                                                            |
| shipments       | List\<Shipment\>  | Shipments fulfilling the physical lines of the order. Empty for orders composed exclusively of digital products.                                                                                                                                                                |

## Relationships

* An `Order` belongs to exactly one `Buyer`.
* An `Order` originates from exactly one confirmed `Cart`.
* An `Order` contains one or more `OrderItem` instances.
* An `Order` is settled by one or more `Payment` instances; only one of them may reach `APPROVED`.
* An `Order` is billed by at most one `Invoice`, issued when the order reaches `PAID`.
* An `Order` is fulfilled by zero or more `Shipment` instances. It has none when all of its lines are digital, and it may have more than one when its physical lines are dispatched from different warehouses.
* An `Order` may be disputed by zero or more `ReturnRequest` instances.

## Business Rules

```text
An Order is created only from a confirmed Cart, and only by a Buyer
whose commercialStatus is ENABLED.                                    (DOMINIO 2, Sección 6.1 step 5)

An Order in state DELIVERED can no longer be modified under any
circumstance.                                                         (Sección 11)

An Order may reach CANCELLED from PENDING_PAYMENT or from PAID, and
never after dispatch. Cancelling an Order releases every inventory
reservation it holds. CANCELLED is inferred; see OrderStatus in
Domain Value Objects.

totalAmount always equals the sum of the subtotals of orderItems,
computed at creation time and never recalculated afterwards.

shippingAddress is a copy taken at checkout. Later changes to the
buyer's addresses never alter an existing Order.

Creating an Order reserves inventory for every line referencing a
variant of a PhysicalProduct.                                         (DOMINIO 6)

Mixed orders — physical and digital lines in the same Order:
  · digital lines are considered delivered as soon as the Order
    reaches PAID;
  · physical lines generate one or more Shipment instances;
  · the Order reaches DISPATCHED when its first Shipment is
    dispatched, and DELIVERED only when every Shipment has been
    delivered;
  · an Order composed exclusively of digital lines generates no
    Shipment and moves directly from PAID to DELIVERED.
```

## Examples of Generated Operations

* `ORDER_PLACEMENT`
* `ORDER_PAYMENT_CONFIRMATION`
* `ORDER_DISPATCH`
* `ORDER_DELIVERY`
* `ORDER_CANCELLATION`

---

# OrderItem

## Description

Represents a single confirmed product variant line within an order, with the quantity and price locked in at the moment of purchase.

**Source:** DOMINIO 7; Sección 6.1 step 5.

## Attributes

| Attribute | Type           | Description                                                                                                                                                            |
| --------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| order     | Order          | Order this line belongs to.                                                                                                                                            |
| variant   | ProductVariant | Product variant purchased by the buyer. The product itself is reachable through `variant.product`.                                                                     |
| quantity  | Integer        | Quantity of the variant requested. Must be greater than zero.                                                                                                          |
| unitPrice | BigDecimal     | Price of the product at the moment the order was confirmed. Kept independent from `Product.price` so that later price changes do not alter historical orders.          |
| subtotal  | BigDecimal     | `quantity * unitPrice`.                                                                                                                                                |

## Relationships

* An `OrderItem` belongs to exactly one `Order`.
* An `OrderItem` references exactly one `ProductVariant`.
* An `OrderItem` referencing a variant of a `PhysicalProduct` is included in exactly one `Shipment`.
* An `OrderItem` may be referenced by zero or more `ReturnItem` instances.

## Business Rules

```text
quantity must be greater than zero.

unitPrice and subtotal are frozen at checkout and are never
recalculated, so that historical orders remain faithful to the
conditions accepted by the buyer.

An OrderItem is never modified after the Order is created.            (Sección 11)
```

---

# Payment

## Description

Represents an attempt to collect the amount of an order from the buyer.

The specification requires payment to be validated before fulfilment begins, and the order lifecycle distinguishes between `PENDING_PAYMENT` and `PAID`, but no entity in the original draft recorded the incoming money — only `Refund` recorded outgoing money. `Payment` closes that gap.

**Design decision — payment attempts, not a single payment:** a payment may be rejected and retried. Modeling a single payment per order would overwrite the failed attempt and lose its trace, which contradicts the traceability requirement of Sección 1 and would make the `REJECTED` and `FAILED` values of `PaymentStatus` meaningless. Each attempt is therefore its own record.

**Design decision — `Payment` is not `Invoice`:** `Invoice` is the commercial document that supports the sale; `Payment` is the financial event that settles it. They have different lifecycles, different responsible parties, and different reasons to exist, so neither replaces the other.

**Source:** DOMINIO 7 (states "Pendiente de Pago" and "Pagado"); Sección 6.1 step 6 — "Se valida el pago y se inicia el flujo de preparación."

## Attributes

| Attribute     | Type          | Description                                                                                                                                                                        |
| ------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| identifier    | String        | Uniquely identifies the payment attempt.                                                                                                                                           |
| order         | Order         | Order this payment settles.                                                                                                                                                        |
| amount        | BigDecimal    | Amount collected, equal to `Order.totalAmount`.                                                                                                                                    |
| currency      | Currency      | Currency in which the payment is expressed. Inferred: see the currency rationale in **Domain Design Rules**.                                                                        |
| paymentStatus | PaymentStatus | Current state of the payment attempt.                                                                                                                                              |
| paymentDate   | LocalDateTime | Date and time the payment attempt was registered. Inferred: consistent with the timestamping of every other traceable business event in this model.                                 |

## Relationships

* A `Payment` settles exactly one `Order`.
* An `Order` may accumulate several `Payment` instances, one per attempt.
* A `Payment` reaching `APPROVED` is what moves its `Order` from `PENDING_PAYMENT` to `PAID`.
* A `Payment` is the financial counterpart of a `Refund`: one records money entering, the other money leaving.

## Business Rules

```text
amount always equals Order.totalAmount; partial payments are not
contemplated by the specification.

At most one Payment per Order may reach APPROVED.

An Order moves to PAID only when one of its Payment instances
reaches APPROVED.                                                     (Sección 6.1 step 6)

A rejected or failed Payment never modifies the Order; it remains as
a historical record of the attempt.

No payment method is modeled: the specification validates that the
payment occurred but does not describe the means used.
```

## Examples of Generated Operations

* `PAYMENT_REGISTRATION`
* `PAYMENT_APPROVAL`
* `PAYMENT_REJECTION`

---

# Invoice

## Description

Represents the commercial billing information associated with a completed sale.

The invoice is the commercial document supporting the transaction, issued once the order has been paid.

**Source:** OBJ-09; Sección 4.1 — "Facturación: Información comercial asociada a las ventas"; Sección 6.1 step 6.

## Attributes

| Attribute   | Type          | Description                                                                                          |
| ----------- | ------------- | ---------------------------------------------------------------------------------------------------- |
| identifier  | String        | Uniquely identifies the invoice.                                                                     |
| order       | Order         | Order this invoice was generated for. Every invoice traces back to exactly one commercial transaction. |
| buyer       | Buyer         | Buyer being billed.                                                                                  |
| issueDate   | LocalDateTime | Date and time the invoice was issued, once the order reached `PAID`.                                  |
| totalAmount | BigDecimal    | Total billed amount, taken from `Order.totalAmount`.                                                 |
| currency    | Currency      | Currency in which the invoice is expressed, taken from the order.                                    |

## Relationships

* An `Invoice` corresponds to exactly one `Order`.
* An `Invoice` is issued to exactly one `Buyer`, who is the buyer of that order.
* An `Invoice` documents the sale; it does not record the collection of funds, which belongs to `Payment`.

## Business Rules

```text
An Invoice is issued only when the Order reaches PAID.                (Sección 6.1 step 6)

At most one Invoice per Order.

An Invoice is immutable once issued; corrections are expressed as new
commercial documents, never as edits.

totalAmount and currency always match those of the Order.
```

## Examples of Generated Operations

* `INVOICE_ISSUANCE`

---

# Shipment

## Description

Represents the logistics process of packing, dispatching, and transporting the physical lines of an order to the buyer.

An order whose physical lines are stocked in different warehouses produces more than one shipment, since each shipment leaves from exactly one origin warehouse.

**Source:** OBJ-10; Sección 4.1 — "Envíos: Procesos logísticos para productos físicos"; Sección 6.1 steps 7–8; Matriz de Responsabilidades ("Gestión de Pedidos → Op. Logístico").

## Attributes

| Attribute         | Type              | Description                                                                                                                                                                                                                       |
| ----------------- | ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identifier        | String            | Uniquely identifies the shipment.                                                                                                                                                                                                 |
| order             | Order             | Order being fulfilled.                                                                                                                                                                                                            |
| items             | List\<OrderItem\> | Order lines included in this shipment. Inferred: an order may be dispatched from several warehouses, so each shipment must state which lines it carries; otherwise partial dispatch could not be represented.                      |
| originWarehouse   | Warehouse         | Warehouse the shipment was dispatched from.                                                                                                                                                                                       |
| logisticsOperator | LogisticsOperator | Operator responsible for the dispatch. Directly implied by the Matriz de Responsabilidades.                                                                                                                                        |
| shipmentStatus    | ShipmentStatus    | Current status of the shipment.                                                                                                                                                                                                   |
| dispatchDate      | LocalDateTime     | Date and time the shipment left the warehouse.                                                                                                                                                                                    |
| deliveryDate      | LocalDateTime     | Date and time the shipment was delivered. Its confirmation is what advances the order towards `DELIVERED` (Sección 6.1 step 8).                                                                                                    |

## Relationships

* A `Shipment` fulfils lines of exactly one `Order`.
* A `Shipment` leaves from exactly one `Warehouse`.
* A `Shipment` is the responsibility of exactly one `LogisticsOperator`.
* A `Shipment` carries one or more `OrderItem` instances, all of them referencing variants of `PhysicalProduct` instances.

## Business Rules

```text
A Shipment is created only after the Order reaches PAID.              (Sección 6.1 steps 6–7)

A Shipment never contains lines referencing a DigitalProduct.         (DOMINIO 5)

Every physical OrderItem of an Order belongs to exactly one Shipment.

Dispatching a Shipment generates an InventoryMovement of type
SALE_OUTBOUND over the corresponding Inventory records.               (DOMINIO 6)

The Order reaches DELIVERED only when every one of its Shipment
instances has been delivered.                                         (Sección 6.1 step 8)
```

## Examples of Generated Operations

* `SHIPMENT_CREATION`
* `SHIPMENT_DISPATCH`
* `SHIPMENT_DELIVERY`

---

# ReturnRequest

## Description

Represents a buyer's request to return one or more purchased items from an order.

The request states exactly which order lines are being returned and in what quantity, through its `ReturnItem` lines. Without that detail the amount to be refunded could not be determined, since a buyer may return only part of an order.

**Source:** OBJ-11; Matriz de Responsabilidades ("Gestión Reembolsos → Comprador"); DOMINIO 6 (movement type "Devolución").

## Attributes

| Attribute    | Type                | Description                                                                                                                                                       |
| ------------ | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| identifier   | String              | Uniquely identifies the return request.                                                                                                                           |
| order        | Order               | Order the return refers to.                                                                                                                                       |
| requestedBy  | Buyer               | Buyer who requested the return, matching the Matriz de Responsabilidades, where only the Comprador row is checked for the initiation of this process.              |
| returnItems  | List\<ReturnItem\>  | Lines being returned. Contains at least one element. Inferred: OBJ-11 speaks of returning "uno o más productos", which requires an explicit line structure.        |
| reason       | String              | Reason given for the return.                                                                                                                                      |
| requestDate  | LocalDateTime       | Date and time the request was created.                                                                                                                            |
| returnStatus | ReturnStatus        | Current status of the return request.                                                                                                                             |

## Relationships

* A `ReturnRequest` refers to exactly one `Order`.
* A `ReturnRequest` is created by exactly one `Buyer`, who must be the buyer of that order.
* A `ReturnRequest` contains one or more `ReturnItem` instances.
* An approved `ReturnRequest` originates exactly one `Refund`.
* An approved `ReturnRequest` over physical items generates `InventoryMovement` instances of type `RETURN`.

## Business Rules

```text
A ReturnRequest may only be created by the Buyer who owns the Order.  (RG-03)

A ReturnRequest may only be created over an Order that has been
delivered.                                                            (OBJ-11, DOMINIO 7)

The returned quantity of a line may never exceed the quantity
purchased in the corresponding OrderItem, discounting quantities
already returned in previous requests.

An approved ReturnRequest over physical items generates an
InventoryMovement of type RETURN.                                     (DOMINIO 6)

A ReturnRequest originates a Refund only when it is approved.
```

## Examples of Generated Operations

* `RETURN_REQUEST_CREATION`
* `RETURN_APPROVAL`
* `RETURN_REJECTION`
* `RETURN_COMPLETION`

---

# ReturnItem

## Description

Represents a single line within a return request: which purchased order line is being returned, and how many units of it.

This is the structure that makes the refund amount computable, since it links each returned unit back to the price frozen in the original order line.

**Source:** OBJ-11 — "devolver uno o más productos"; line structure inferred, mirroring `OrderItem`, so that partial returns and their refund amount can be represented.

## Attributes

| Attribute         | Type       | Description                                                                                                                                                              |
| ----------------- | ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| returnRequest     | ReturnRequest | Return request this line belongs to.                                                                                                                                  |
| orderItem         | OrderItem  | Original order line being returned. The variant and its purchase price are reachable through it.                                                                         |
| quantity          | Integer     | Number of units being returned. Must be greater than zero and no greater than the quantity purchased.                                                                   |
| refundableAmount  | BigDecimal | `quantity * orderItem.unitPrice`. Inferred: computed from the frozen purchase price so that the refund reimburses what the buyer actually paid, not the current price.    |

## Relationships

* A `ReturnItem` belongs to exactly one `ReturnRequest`.
* A `ReturnItem` refers to exactly one `OrderItem`, which must belong to the order of that return request.
* The `refundableAmount` values of all `ReturnItem` instances of a request add up to the amount of the resulting `Refund`.

## Business Rules

```text
quantity must be greater than zero and must not exceed the quantity
of the referenced OrderItem, discounting units already returned.

refundableAmount is always computed from OrderItem.unitPrice, the
price frozen at checkout, never from the current Product price.

The referenced OrderItem must belong to the Order of the
ReturnRequest.
```

---

# Refund

## Description

Represents the monetary reimbursement processed as a result of an approved return.

Modeled as a separate entity from `ReturnRequest` because the specification's own objectives separate "devoluciones" — the return of the product — from "reembolsos" — the movement of money — and because the Matriz de Responsabilidades assigns the refund step specifically to the Administrator, while the return is initiated by the Buyer.

`Refund` is the financial counterpart of `Payment`: one records money leaving the marketplace, the other money entering it.

**Source:** OBJ-11; Matriz de Responsabilidades ("Gestión Reembolsos → Comprador solicita, Admin ejecuta").

## Attributes

| Attribute     | Type          | Description                                                                                                                              |
| ------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| identifier    | String        | Uniquely identifies the refund.                                                                                                          |
| returnRequest | ReturnRequest | Return request that originated this refund.                                                                                              |
| amount        | BigDecimal    | Amount reimbursed to the buyer, equal to the sum of the `refundableAmount` of the request's `ReturnItem` lines.                           |
| currency      | Currency      | Currency in which the refund is expressed, taken from the original order. Inferred: see the currency rationale in **Domain Design Rules**. |
| refundStatus  | RefundStatus  | Current status of the refund.                                                                                                            |
| processedBy   | Administrator | Administrator who processed the refund, per the Matriz de Responsabilidades.                                                              |
| processDate   | LocalDateTime | Date and time the refund was processed.                                                                                                  |

## Relationships

* A `Refund` originates from exactly one approved `ReturnRequest`.
* A `Refund` is processed by exactly one `Administrator`.
* A `Refund` reimburses the `Buyer` who created the return request, reached through the request.
* A `Refund` is the reverse financial movement of the `Payment` that settled the original order.

## Business Rules

```text
A Refund exists only for a ReturnRequest in state APPROVED.

amount always equals the sum of the refundableAmount of the return
lines, and therefore never exceeds what the buyer actually paid.

currency always matches the currency of the original Order.

Only an Administrator may process a Refund.                           (Matriz de Responsabilidades)
```

## Examples of Generated Operations

* `REFUND_PROCESSING`
* `REFUND_REJECTION`

---

# Operation

## Description

Represents a significant business action executed within the marketplace.

Operations provide traceability between users, business entities, and audit records. A business entity may generate multiple operations during its lifecycle.

An operation represents an event or action that occurred; it is distinct from the current status of the affected entity.

For example:

```text
Order.orderStatus = PAID
```

represents the current state of the order, while:

```text
Operation.operationType = ORDER_PAYMENT_CONFIRMATION
```

represents the action that caused the state change.

**Design decision — typed reference instead of a common abstract root:** the banking reference points `Operation.affectedProduct` at `BankingProduct`, an abstract root that all its products share. NexusMarket has no equivalent business concept: significant events affect entities as different as `Order`, `Payment`, `Inventory`, `Shipment`, `ReturnRequest`, `Refund`, `Product`, `Seller`, and `Warehouse`, which share no common business meaning. Introducing an artificial technical root to group them would invent a concept the specification does not support. Instead, the affected entity is identified by a controlled catalog (`AffectedEntityType`) plus its identifier, which keeps the reference typed and domain-controlled without inventing a false hierarchy.

**Source:** OBJ-12; Sección 1 — "garantizando trazabilidad y coordinación entre todos los participantes"; RG-01. Pattern adopted from the banking reference `Operation` → `AuditLog` chain.

## Attributes

| Attribute          | Type               | Description                                                             |
| ------------------ | ------------------ | ----------------------------------------------------------------------- |
| operationId        | String             | Uniquely identifies the operation.                                      |
| operationType      | OperationType      | Category of the business operation.                                     |
| executionDate      | LocalDateTime      | Date and time the operation occurred.                                   |
| performedBy        | User               | User responsible for executing the operation.                           |
| affectedEntityType | AffectedEntityType | Type of business entity affected by the operation.                      |
| affectedEntityId   | String             | Identifier of the specific instance affected by the operation.          |

## Relationships

* Each `Operation` is performed by exactly one `User`.
* Each `Operation` affects exactly one business entity, identified by `affectedEntityType` and `affectedEntityId`.
* A business entity may generate zero or more `Operation` instances during its lifecycle.
* Each significant `Operation` must be recorded in the `AuditLog`.

## Business Rules

```text
Every significant business action performed within the marketplace
must generate an Operation.

Every Operation must be executed by an authenticated User.            (RG-01)

Every Operation must be recorded in the AuditLog.

An Operation records an event that occurred; it is never modified
afterwards, because the event itself does not change.
```

---

# AuditLog

## Description

Represents the immutable audit trail of the marketplace.

Each audit record stores historical information about a significant business operation, together with the role held by the user at the moment of execution.

Audit records are intended to be persisted in a NoSQL database to support flexible, operation-specific details and long-term historical traceability.

Audit records are append-only and must not be modified or deleted after being persisted.

This entity, together with `Operation`, is what satisfies OBJ-12 ("Consolidar información administrativa para consulta") and the traceability commitment stated in Sección 1, both of which describe cross-cutting administrative traceability rather than the state of any single business transaction.

**Source:** OBJ-12; Sección 1. Introducción y Contexto; Sección 5 (Supervisor as a consultation profile).

## Attributes

| Attribute          | Type                  | Description                                                  |
| ------------------ | --------------------- | ------------------------------------------------------------ |
| auditId            | String                | Uniquely identifies the audit record.                        |
| operationType      | OperationType         | Type of business operation recorded.                         |
| operationDate      | LocalDateTime         | Timestamp when the event occurred.                           |
| performedBy        | User                  | User responsible for the operation.                          |
| userRole           | SystemRole            | Role of the user at the time of execution.                   |
| affectedEntityType | AffectedEntityType    | Type of business entity involved in the operation.           |
| affectedEntityId   | String                | Identifier of the specific instance involved.                |
| details            | Map\<String, Object\> | Flexible document containing operation-specific information. |

## Relationships

* Each `AuditLog` record corresponds to exactly one `Operation`.
* Each `AuditLog` record references the `User` who performed the operation and the `SystemRole` held at that moment.
* `AuditLog` records are the primary information consulted by the `Supervisor` and by administrative reporting.

## Business Rules

* Audit records are immutable.
* Audit records are append-only.
* An audit record cannot be deleted after persistence.
* Every significant business operation must produce an audit record.
* The `userRole` must represent the role applicable at the time the operation was performed, not the user's current role.
* The `details` field may contain operation-specific information required for traceability.

---

# Domain Lifecycle Relationship

The general lifecycle of any significant business action in the marketplace is:

```text
Business entity
      │
      │ significant business action
      ▼
  Operation
      │
      │ audit registration
      ▼
  AuditLog
```

For example, when the payment of an order is confirmed:

```text
Order
   │
   │ a Payment reaches APPROVED
   ▼
 PAID
   │
   ├── Operation
   │      operationType      = ORDER_PAYMENT_CONFIRMATION
   │      affectedEntityType = ORDER
   │      affectedEntityId   = Order.identifier
   │      performedBy        = User
   │
   └── AuditLog
          operationType      = ORDER_PAYMENT_CONFIRMATION
          affectedEntityType = ORDER
          affectedEntityId   = Order.identifier
          performedBy        = User
          userRole           = BUYER
          details            = operation-specific information
```

Similarly, when a shipment is dispatched from a warehouse:

```text
Shipment
   │
   │ shipmentStatus changes
   ▼
IN_TRANSIT
   │
   ├── InventoryMovement
   │      movementType = SALE_OUTBOUND
   │
   ├── Operation
   │      operationType      = SHIPMENT_DISPATCH
   │      affectedEntityType = SHIPMENT
   │      affectedEntityId   = Shipment.identifier
   │      performedBy        = User
   │
   └── AuditLog
          operationType      = SHIPMENT_DISPATCH
          affectedEntityType = SHIPMENT
          affectedEntityId   = Shipment.identifier
          performedBy        = User
          userRole           = LOGISTICS_OPERATOR
          details            = operation-specific information
```

And when a refund is processed after an approved return:

```text
ReturnRequest
   │
   │ returnStatus changes
   ▼
APPROVED
   │
   ├── Refund
   │      amount = Σ ReturnItem.refundableAmount
   │      refundStatus = PROCESSED
   │
   ├── InventoryMovement
   │      movementType = RETURN
   │
   ├── Operation
   │      operationType      = REFUND_PROCESSING
   │      affectedEntityType = REFUND
   │      affectedEntityId   = Refund.identifier
   │      performedBy        = User
   │
   └── AuditLog
          operationType      = REFUND_PROCESSING
          affectedEntityType = REFUND
          affectedEntityId   = Refund.identifier
          performedBy        = User
          userRole           = ADMINISTRATOR
          details            = operation-specific information
```

---

# Domain Design Rules

## Inheritance Represents Genuine Specialization

Inheritance is used only where the business itself distinguishes subtypes with different rules: user roles, warehouse ownership, and product nature. It is never used as a technical grouping mechanism, which is why `Operation` identifies the affected entity through a controlled catalog rather than through an invented abstract root.

## Explicit Object References

Entities reference other entities directly rather than storing loose identifier fields. The only exception is `Operation.affectedEntityId` and `AuditLog.affectedEntityId`, where the referenced entity is polymorphic by nature and is therefore qualified by `AffectedEntityType`.

## Value Objects Instead of Primitives

Every controlled business concept — roles, statuses, types, currencies — is represented by a Value Object inheriting from `DomainCatalog`, never by an arbitrary string. The complete catalog is defined in *Domain Value Objects*.

## The Sellable Unit Is the Variant

`Product` is a catalog concept; `ProductVariant` is the sellable unit. Stock, cart lines, and order lines always reference a variant, never a product. A product with no real variation is registered with a single default variant, so that no optional product/variant path exists anywhere in the model.

## Frozen Commercial Conditions

An `Order` copies, rather than references, everything that must remain faithful to the moment of purchase: `unitPrice`, `subtotal`, `totalAmount`, and `shippingAddress`. Later changes to the catalog or to the buyer's profile never alter a historical order. `ReturnItem.refundableAmount` follows the same principle, computing from the frozen price rather than the current one.

## Monetary Amounts Are Always Denominated

Every entity holding a monetary amount also holds the `Currency` in which it is expressed: `Product`, `Order`, `Payment`, `Invoice`, and `Refund`.

**Rationale — why `Currency` applies here.** The specification never names a currency, which could suggest the concept is out of scope. It is not: the marketplace intermediates between independent third-party sellers and buyers, issues invoices, collects payments, and returns money through refunds. A `BigDecimal` with no denomination is ambiguous the moment more than one currency exists, and an amount refunded must be provably the same amount that was charged — which cannot be verified if neither side is denominated. The banking reference already establishes `Currency` as a `DomainCatalog` Value Object for exactly this reason, and the same argument holds here. Modeling it now costs one Value Object; omitting it would require changing every monetary attribute in the model later.

## Money In and Money Out Are Both Recorded

`Payment` records money entering the marketplace and `Refund` records money leaving it. `Invoice` is the commercial document supporting the sale and does not replace either of them: a document proving what was sold is not a record of the funds moving.

## Immutability of Concluded Facts

Records that represent something that already happened are never edited: `InventoryMovement`, `Payment`, `Invoice`, `Operation`, `AuditLog`, and any `Order` in state `DELIVERED`. Corrections are expressed as new records — an `ADJUSTMENT` movement, a new payment attempt, a new commercial document — never as modifications of the original.

## Every Significant Action Is Traceable

Every significant business action generates an `Operation`, and every `Operation` is recorded in the `AuditLog` together with the role held by the user at that moment. This chain is what satisfies the traceability commitment of Sección 1 and the administrative consultation objective OBJ-12.

## Role-Scoped Access

No participant administers information outside their own role (RG-03). This constraint is stated in the business rules of each entity — buyers over their own orders, sellers over their own products and inventory, administrators over sellers, warehouses, and refunds, supervisors in read-only mode — so that authorization is a property of the domain and not only of the application layer.

## Traceability to the Specification

Every entity states the section of the *Especificación Funcional del Negocio* it originates from, and every attribute that is not literally stated in the specification is marked as inferred with its justification. This is a deliberate addition over the banking reference: the reasoning that produced the model travels inside the model, so a reviewer never has to reconstruct it.
