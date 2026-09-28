# Catalog Services

## Introduction

This document defines the services belonging to the **Catalog Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Registering physical and digital products with their variants.
- Maintaining the commercial information of a product and adding variants to it.
- Managing the publication lifecycle of a product: publication, suspension, and discontinuation.
- Consulting the public catalog and individual products.

**Source:** OBJ-05 ("Gestionar el catálogo de productos"); DOMINIO 5. Gestión del Catálogo ("El catálogo diferencia entre productos físicos (requieren inventario y despacho) y productos digitales (entrega inmediata tras pago)"; "Variantes: Diferencias de color, talla, modelo, etc."; "Estado: Publicado, Suspendido o Descontinuado"); Sección 5 ("Vendedor: Responsable de registrar y administrar sus productos"); Sección 6.1 steps 2–4; Matriz de Responsabilidades ("Registro Productos → Vendedor").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

`Product` is an abstract Domain Model with two specializations; `ProductVariant` is the sellable unit:

```text
Product (Abstract)
├── identifier
├── name, description
├── productType     : ProductType     PHYSICAL | DIGITAL
├── seller          : Seller
├── price, currency : BigDecimal, Currency
├── variants        : List<ProductVariant>, at least one
└── productStatus   : ProductStatus   DRAFT | PUBLISHED | SUSPENDED | DISCONTINUED
│
├── PhysicalProduct     requires inventory and dispatch
└── DigitalProduct      delivered immediately after payment

ProductVariant
├── variantId
├── sku                 unique across the platform
├── product : Product
├── attributeName       e.g. "Color"   (absent in a default variant)
└── attributeValue      e.g. "Rojo"    (absent in a default variant)
```

Relationships are represented using Domain Models — `Product.seller : Seller`, `ProductVariant.product : Product` — never as loose identifiers.

The product lifecycle, defined in *Domain Value Objects*, is:

```text
DRAFT ─────────────────────────> DISCONTINUED
  │
  ▼
PUBLISHED ───> SUSPENDED ───> PUBLISHED
  │
  └─────────────────────────────> DISCONTINUED
```

Every product is registered in `DRAFT` because Sección 6.1 registers the product (step 2) and its inventory (step 3) before publishing it (step 4).

---

# Service Design Principles

## Domain Model Parameters

### Incorrect

```java
registerProduct(
    String sellerId,
    String name,
    BigDecimal price,
    String type
);
```

### Correct

```java
registerProduct(User requestingUser, Product newProduct);
```

The concrete specialization — `PhysicalProduct` or `DigitalProduct` — is the product type; the seller is the requesting user; the variants travel inside the product.

### Incorrect

```java
publishProduct(String productId);
```

### Correct

```java
publishProduct(User requestingUser, Product product);
```

## The Requesting User

Every service of this subdomain receives, as its first parameter, the `User` who is executing it.

---

# Domain Behavior

| Method                                                           | Rule it enforces                                                                                                   |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `void Product.initializeDraft(Seller owner)`                     | Validates the product for registration (below), sets its owner, sets `productType` from its specialization, sets `DRAFT`, and links every variant to it. |
| `boolean Product.isPublished()`                                  | Only a `PUBLISHED` product is visible and may be added to a cart (DOMINIO 5).                                      |
| `void Product.publish()`                                         | Allowed from `DRAFT` and `SUSPENDED` only.                                                                         |
| `void Product.suspend()`                                         | Allowed from `PUBLISHED` only.                                                                                     |
| `void Product.discontinue()`                                     | Allowed from `DRAFT` and `PUBLISHED` only; `DISCONTINUED` is terminal.                                             |
| `List<String> Product.updateDetails(String name, String description, BigDecimal price)` | Rejected on a discontinued product; applies the same validation as registration; returns the attributes that changed. |
| `void Product.addVariant(ProductVariant variant)`                | Rejected on a discontinued product; validates the variant against the existing ones and links it to the product.   |
| `boolean Product.requiresInventory()`                            | `true` for a `PhysicalProduct`, `false` for a `DigitalProduct` (DOMINIO 5).                                         |

## Registration Validation

Applied by `initializeDraft` and, for the relevant attributes, by `updateDetails` and `addVariant`:

* `name` is not blank. Inferred in the *Domain Model*: a catalog cannot be browsed without a display name.
* `price` is present and greater than zero. Inferred: the specification requires payment of every order (Sección 6.1 step 6), and a product with no positive price produces no payment to validate.
* `currency` is present (*Domain Model*, **Monetary Amounts Are Always Denominated**).
* There is at least one variant (*Domain Model*, `Product`).
* Every variant has a non-blank `sku`, and no two variants of the product share one.
* A variant's `attributeName` and `attributeValue` are either both present or both absent.
* A product with more than one variant has attributes on every variant, and no two variants share the same name/value pair: two variants without distinguishing attributes would be the same sellable unit twice.

---

# 1. Register Product

## Description

Registers a new physical or digital product in the catalog, together with at least one variant.

A product with no real variation is registered with a single default variant: a variant with its `sku` and without attributes. The product starts in `DRAFT` and is not visible in the public catalog until it is published.

**Performed by:** Seller, over their own products.

**Source:** DOMINIO 5; Sección 5; Sección 6.1 step 2; Matriz de Responsabilidades ("Registro Productos → Vendedor").

---

## Input

```text
requestingUser : User
newProduct     : Product    PhysicalProduct | DigitalProduct, with name, description, price, currency, variants
```

`newProduct` carries no identifier, seller, product type, or status: they are established by the service.

---

## Processing

```text
requestingUser + newProduct
 │
 ▼
Validate User Status ──> Validate Role Permission (SELLER)
 │
 ▼
newProduct present? ──> newProduct.initializeDraft(seller)
 │
 ▼
For each variant: ProductRepositoryPort.existsBySku ──> already used? ──> reject
 │
 ▼
ProductRepositoryPort.save ──> product and variants (identifiers assigned)
 │
 ▼
Register Operation and Audit (PRODUCT_REGISTRATION)
```

The seller who owns the product is always the requesting seller: a seller cannot register a product for another seller (RG-03).

---

## Operation and Audit

| Field                | Value                                     |
| -------------------- | ----------------------------------------- |
| `operationType`      | `PRODUCT_REGISTRATION`                    |
| `affectedEntityType` | `PRODUCT`                                 |
| `affectedEntityId`   | `identifier` of the product               |
| `performedBy`        | the seller                                |
| `details`            | `productType`, `variantCount`             |

---

# 2. Update Product

## Description

Updates the commercial information of a product — name, description, and price — and adds new variants to it.

Existing variants are never modified or removed: they may already be referenced by inventory records, cart lines, and order lines, and changing them would alter what those records mean. A variant that should no longer be sold is handled by its stock, not by editing it.

Price changes never alter existing orders, because each order line holds the price frozen at checkout.

A new variant added to a published physical product becomes visible immediately but cannot be sold until stock is registered for it, since reservation requires an inventory record (`inventory-services.md`).

**Performed by:** Seller, over their own products.

**Source:** Sección 5 ("registrar y administrar sus productos"); DOMINIO 5 ("Variantes"); RG-03.

---

## Input

```text
requestingUser : User
product        : Product    identifier, name, description, price, and — optionally — new variants without variantId
```

---

## Processing

```text
requestingUser + product
 │
 ▼
Validate User Status ──> Validate Role Permission (SELLER)
 │
 ▼
ProductRepositoryPort.findById ──> stored product? ──no──> EntityNotFoundException
 │
 ▼
Validate Seller Ownership (storedProduct.seller)
 │
 ▼
changedFields = storedProduct.updateDetails(name, description, price)
 │
 ▼
For each new variant: existsBySku? ──> reject;  storedProduct.addVariant(variant)
 │
 ▼
Nothing changed and no variant added? ──yes──> InvalidProductException
 │
 ▼
ProductRepositoryPort.update
 │
 ▼
Register Operation and Audit (PRODUCT_UPDATE)
```

---

## Operation and Audit

| Field                | Value                                          |
| -------------------- | ---------------------------------------------- |
| `operationType`      | `PRODUCT_UPDATE`                               |
| `affectedEntityType` | `PRODUCT`                                      |
| `affectedEntityId`   | `identifier` of the product                    |
| `performedBy`        | the seller                                     |
| `details`            | `changedFields`, `addedVariantCount`           |

---

# 3. Publish Product

## Description

Makes a product visible in the public catalog by changing its status to `PUBLISHED`, from `DRAFT` (first publication) or from `SUSPENDED` (return to the catalog).

A physical product can only be published when every one of its variants has an inventory record in at least one warehouse (*Domain Model*, `PhysicalProduct`; Sección 6.1 step 3 before step 4). A digital product has no such requirement.

**Performed by:** Seller, over their own products.

**Source:** DOMINIO 5; Sección 6.1 steps 3–4.

---

## Processing

```text
requestingUser + product
 │
 ▼
Validate User Status ──> Validate Role Permission (SELLER)
 │
 ▼
ProductRepositoryPort.findById ──> Validate Seller Ownership
 │
 ▼
storedProduct.requiresInventory()?
 │
 └── yes ──> every variant: InventoryRepositoryPort.existsByVariant? ──no──> InvalidProductException
 │
 ▼
storedProduct.publish()
 │
 ▼
ProductRepositoryPort.update
 │
 ▼
Register Operation and Audit (PRODUCT_PUBLICATION)
```

The inventory rule requires a record, not available units: a product may be published with its stock momentarily at zero, as long as it has a place in some warehouse where its stock is counted.

---

## Operation and Audit

| Field                | Value                             |
| -------------------- | --------------------------------- |
| `operationType`      | `PRODUCT_PUBLICATION`             |
| `affectedEntityType` | `PRODUCT`                         |
| `affectedEntityId`   | `identifier` of the product       |
| `performedBy`        | the seller                        |
| `details`            | `previousStatus`                  |

---

# 4. Suspend Product

## Description

Temporarily removes a published product from the public catalog by changing its status to `SUSPENDED`. The suspension is reversible through **Publish Product**.

Carts that already contain a variant of the product keep it, but the product cannot be ordered while suspended: **Place Order** requires every variant to belong to a published product (`order-services.md`).

**Performed by:** Seller, over their own products.

**Source:** DOMINIO 5 ("Suspendido").

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SELLER)
 │
 ▼
ProductRepositoryPort.findById ──> Validate Seller Ownership
 │
 ▼
storedProduct.suspend() ──> ProductRepositoryPort.update
 │
 ▼
Register Operation and Audit (PRODUCT_SUSPENSION)
```

---

## Operation and Audit

`PRODUCT_SUSPENSION`, `PRODUCT`, product `identifier`, performed by the seller, details `previousStatus`.

---

# 5. Discontinue Product

## Description

Permanently removes a product from active sale by changing its status to `DISCONTINUED`, from `DRAFT` or from `PUBLISHED`. The discontinuation is terminal, and a discontinued product can no longer be updated.

A suspended product must be published again before it can be discontinued. This follows the lifecycle defined in *Domain Value Objects*, which gives `SUSPENDED` a single exit towards `PUBLISHED`.

Existing orders, invoices, and returns referencing the product are not affected.

**Performed by:** Seller, over their own products.

**Source:** DOMINIO 5 ("Descontinuado").

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SELLER)
 │
 ▼
ProductRepositoryPort.findById ──> Validate Seller Ownership
 │
 ▼
storedProduct.discontinue() ──> ProductRepositoryPort.update
 │
 ▼
Register Operation and Audit (PRODUCT_DISCONTINUATION)
```

---

## Operation and Audit

`PRODUCT_DISCONTINUATION`, `PRODUCT`, product `identifier`, performed by the seller, details `previousStatus`.

---

# 6. Consult Catalog

## Description

Retrieves the products visible in the public catalog: those whose status is `PUBLISHED`, with their variants.

**Performed by:** Any authenticated user.

**Source:** Sección 6.1 step 4 ("Los productos se hacen visibles en el catálogo público"); DOMINIO 5.

---

## Processing

```text
Validate User Status
 │
 ▼
ProductRepositoryPort.findPublished
 │
 ▼
List<Product>
```

Consultation changes nothing and generates no `Operation`.

---

# 7. Consult Product

## Description

Retrieves the full information of a product and its variants.

**Performed by:** The owning seller, over their products in any status; any other authenticated user, over published products only.

**Source:** DOMINIO 5; RG-03.

---

## Processing

```text
Validate User Status
 │
 ▼
ProductRepositoryPort.findById ──> stored product? ──no──> EntityNotFoundException
 │
 ▼
Requesting user is the owning seller? ──yes──> Product
 │
 no
 │
 ▼
storedProduct.isPublished()? ──no──> EntityNotFoundException
 │
 ▼
Product
```

A product that is not published is reported as not found to anyone but its owner, so that the existence of drafts and suspended products is not disclosed.

---

# Output Ports

```text
ProductRepositoryPort
InventoryRepositoryPort   (defined in inventory-services.md; used only by Publish Product)
```

---

# ProductRepositoryPort

## Description

Defines the persistence operations required over products and their variants. Implemented by the MySQL persistence adapter.

## Contract

```java
public interface ProductRepositoryPort {

    <T extends Product> T save(T product);

    Optional<Product> findById(Product product);

    List<Product> findPublished();

    boolean existsBySku(ProductVariant variant);

    void update(Product product);
}
```

`save` assigns the product `identifier` and every `variantId`, and returns the same specialization it received. `findById` returns the concrete specialization with its seller and variants. `update` persists the details, the status, and any variant added.

---

# InventoryRepositoryPort

This subdomain uses only:

```java
boolean existsByVariant(ProductVariant variant);
```

which answers whether the variant has an inventory record in any warehouse.

---

# Validation Matrix

| Service             | Requesting user | Role validation | Ownership                          | Existence (Output Port)          | Uniqueness (Output Port)  | Other                                   |
| ------------------- | --------------- | --------------- | ---------------------------------- | -------------------------------- | ------------------------- | --------------------------------------- |
| Register Product    | ACTIVE          | SELLER          | The seller becomes the owner       | —                                | `existsBySku`             | Registration validation                 |
| Update Product      | ACTIVE          | SELLER          | Yes                                | `findById`                       | `existsBySku` (new variants) | Not discontinued                     |
| Publish Product     | ACTIVE          | SELLER          | Yes                                | `findById`                       | —                         | Inventory for every physical variant    |
| Suspend Product     | ACTIVE          | SELLER          | Yes                                | `findById`                       | —                         | Lifecycle                               |
| Discontinue Product | ACTIVE          | SELLER          | Yes                                | `findById`                       | —                         | Lifecycle                               |
| Consult Catalog     | ACTIVE          | Any             | —                                  | `findPublished`                  | —                         | —                                       |
| Consult Product     | ACTIVE          | Any             | Owner sees any status              | `findById`                       | —                         | Others see published only               |

## Service-to-Port Matrix

| Service             | ProductRepositoryPort                    | InventoryRepositoryPort |
| ------------------- | ---------------------------------------- | ----------------------- |
| Register Product    | `existsBySku`, `save`                    |                         |
| Update Product      | `findById`, `existsBySku`, `update`      |                         |
| Publish Product     | `findById`, `update`                     | `existsByVariant`       |
| Suspend Product     | `findById`, `update`                     |                         |
| Discontinue Product | `findById`, `update`                     |                         |
| Consult Catalog     | `findPublished`                          |                         |
| Consult Product     | `findById`                               |                         |

---

# Input Ports

| Service                                                   | Exposed to                  |
| --------------------------------------------------------- | --------------------------- |
| Register, Update, Publish, Suspend, Discontinue Product   | Seller                      |
| Consult Catalog, Consult Product                          | Every role                  |

---

# Exceptions

| Exception                          | Raised when                                                                                                        |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `InvalidProductException`          | The product is missing or fails the registration validation; a SKU is already used; the product is discontinued; an update changes nothing; a physical variant has no inventory at publication. |
| `InvalidStatusTransitionException` | Publish, suspend, or discontinue is requested from a status that does not allow it.                                |
| `EntityNotFoundException`          | The product does not exist, or it is not published and the requester is not its owner.                             |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, is not a seller where one is required, or does not own the product.           |

---

# Business Rules Summary

## BR-CAT-001 — Only a seller registers and administers products, and only their own

(Sección 5; Matriz de Responsabilidades; RG-03)

## BR-CAT-002 — Every product has at least one variant, and the variant is the sellable unit

(*Domain Model*, `Product` and `ProductVariant`)

## BR-CAT-003 — SKUs are unique across the platform

(*Domain Model*, `ProductVariant`)

## BR-CAT-004 — Every product starts in DRAFT

(Sección 6.1 steps 2–4)

## BR-CAT-005 — A physical product is published only when every variant has inventory

(Sección 6.1 step 3; *Domain Model*, `PhysicalProduct`)

## BR-CAT-006 — Only PUBLISHED products are visible in the catalog

(DOMINIO 5; Sección 6.1 step 4)

## BR-CAT-007 — Status changes follow the ProductStatus lifecycle

## BR-CAT-008 — A discontinued product can no longer be modified

## BR-CAT-009 — Existing variants are never modified

Only new variants can be added.

## BR-CAT-010 — Price changes never alter existing orders

(*Domain Model*, **Frozen Commercial Conditions**)

---

# Java Implementation

```text
domain/
├── models/
│   ├── Product.java                   initializeDraft, isPublished, publish, suspend, discontinue, updateDetails, addVariant, requiresInventory
│   ├── PhysicalProduct.java           requiresInventory() returns true
│   └── DigitalProduct.java            requiresInventory() returns false
├── exceptions/
│   └── InvalidProductException.java
├── ports/out/
│   ├── ProductRepositoryPort.java
│   └── InventoryRepositoryPort.java   (existsByVariant; completed in inventory-services.md)
└── services/catalog/
    ├── RegisterProductService.java       Product registerProduct(User requestingUser, Product newProduct)
    ├── UpdateProductService.java         Product updateProduct(User requestingUser, Product product)
    ├── PublishProductService.java        Product publishProduct(User requestingUser, Product product)
    ├── SuspendProductService.java        Product suspendProduct(User requestingUser, Product product)
    ├── DiscontinueProductService.java    Product discontinueProduct(User requestingUser, Product product)
    ├── ConsultCatalogService.java        List<Product> consultCatalog(User requestingUser)
    └── ConsultProductService.java        Product consultProduct(User requestingUser, Product product)
```

Each service is one class with one public method, annotated with `@Service` and `@RequiredArgsConstructor`, as in the banking reference.

---

# Architectural Constraints

1. `Product` is an abstract Domain Model specialized as `PhysicalProduct` and `DigitalProduct`; `ProductVariant` is the sellable unit.
2. Services receive Domain Models, never primitive identifiers, DTOs, or persistence entities.
3. Every service receives the requesting `User`.
4. The owner of a product is the seller who registers it and never changes.
5. The product type is determined by the specialization, never by the client.
6. Status changes go through `Product` behavior and follow the `ProductStatus` lifecycle.
7. Existing variants are never modified.
8. Every state change registers an `Operation` and its `AuditLog`.
9. All catalog rules must remain testable without infrastructure.
