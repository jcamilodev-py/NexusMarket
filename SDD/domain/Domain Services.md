# Services

## Introduction

This document provides a conceptual overview of the services that compose the NexusMarket marketplace platform.

The services described here define the main business capabilities exposed by the system. At this level, each service is described only in terms of its purpose, its responsibility within the domain, the participant who performs it, and the section of the *Especificación Funcional del Negocio - NexusMarket* it originates from.

The detailed definition of each service — including inputs, outputs, business rules, validations, authorization requirements, domain interactions, exceptions, persistence considerations, and technical implementation — is documented in separate files organized by **subdomain**.

The subdomains follow the functional objectives of the specification one to one, so that every service traces back to an objective without an intermediate mapping. Two additional cross-cutting subdomains, taken from the banking reference, support all the others:

| Subdomain                              | Objective          |
| -------------------------------------- | ------------------ |
| **User and Authentication Management** | OBJ-01             |
| **Seller Management**                  | OBJ-02             |
| **Buyer Management**                   | OBJ-03             |
| **Warehouse Management**               | OBJ-04             |
| **Catalog Management**                 | OBJ-05             |
| **Inventory Management**               | OBJ-06             |
| **Cart Management**                    | OBJ-07             |
| **Order Management**                   | OBJ-08             |
| **Payment and Billing Management**     | OBJ-09             |
| **Shipment Management**                | OBJ-10             |
| **Return and Refund Management**       | OBJ-11             |
| **Administrative Reporting**           | OBJ-12             |
| **Operation and Audit Management**     | Cross-cutting — OBJ-12; Sección 1 (traceability) |
| **Authorization**                      | Cross-cutting — RG-01, RG-02, RG-03; Matriz de Responsabilidades |

Each service states the participant who performs it under **Performed by**. Services marked **Internal** are never invoked directly by a participant: they are triggered by another service as a consequence of a business event, in the same way the banking reference registers operations and audit records from within the services that originate them.

Where the specification does not state explicitly who performs an action, the assignment is marked as inferred and justified inline.

---

# Conventions Shared by All Services

These conventions apply to every service and are detailed in each subdomain document.

* **Authenticated execution.** Every service is executed by an authenticated `User` whose `status` is `ACTIVE` (RG-01). The only two exceptions are **Register Buyer** and **Login**, because they are precisely the services that produce an authenticated user; requiring authentication for them would make the platform unreachable. Inferred interpretation of RG-01, consistent with the banking reference, whose public access port exposes registration and login only.
* **Role-scoped access.** Every service validates that the role of the requesting user is allowed to perform it and, where applicable, that the user owns the information being accessed (RG-02, RG-03).
* **Traceability.** Every service that changes the state of a business entity generates an `Operation` and its `AuditLog` record. Consultation services change nothing and therefore generate neither, as in the banking reference.
* **Domain Models as parameters.** Services receive and return Domain Models, never REST DTOs, persistence entities, or loose primitive identifiers, as established by the banking reference.

---

# User and Authentication Management Services

**Source:** OBJ-01; DOMINIO 1. Administración de Usuarios; Sección 5. Participantes del Negocio; RG-01, RG-02; Sección 11.

## Register Staff User

Creates a system user for one of the internal roles of the marketplace: `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, or `SUPERVISOR`.

Buyers are created through **Register Buyer** and sellers through **Register Seller**, so this service never creates either of those two roles. It validates that the identity document and the email address are unique across the platform.

**Performed by:** Administrator. Inferred: OBJ-01 requires administering the information of every user of the marketplace, but the specification only assigns seller registration explicitly; the Administrator is the only administrative profile defined in Sección 5.

## Login

Authenticates a user through their email address and password, and establishes the authenticated identity required by every other service.

Only a user whose `status` is `ACTIVE` may authenticate. The password is verified against a stored hash, never against a plain value.

This service defines the business rule of *who* may authenticate. The technical mechanism that represents the authenticated session — the token — is excluded by Sección 3.2 ("mecanismos de autenticación técnica") and is therefore delegated to an Output Port.

**Performed by:** Any registered user, without prior authentication.

**Inferred:** relies on the `passwordHash` attribute of `User`, itself inferred in the *Domain Model*. Justified by DOMINIO 1, which describes this domain as "la base de autenticación e identificación" and the email as the "medio principal de acceso", and by RG-01, which requires an authenticated user for every operation.

## Consult User

Retrieves the information of a system user according to the permissions of the requesting user.

**Performed by:** The user themself over their own information; Administrator and Supervisor over any user.

## Change User Status

Changes the operational status of a user among `ACTIVE`, `INACTIVE`, and `BLOCKED`.

A user who is not `ACTIVE` can no longer authenticate nor execute any service.

**Performed by:** Administrator. Inferred, for the same reason as **Register Staff User**.

---

# Seller Management Services

**Source:** OBJ-02; DOMINIO 3. Gestión de Vendedores; Sección 6.1 step 1; Matriz de Responsabilidades ("Registro Vendedores → Admin").

## Register Seller

Onboards a new seller together with their first warehouse, as a single business action.

Creates the seller user with their commercial identity — legal business name, tax identification, and trade name — and the first `SellerWarehouse` owned by that seller. Neither record can exist without the other.

Sellers cannot register themselves.

**Performed by:** Administrator.

## Consult Seller

Retrieves the information of a seller, including their commercial identity and their warehouses.

**Performed by:** The seller themself; Administrator and Supervisor over any seller.

## Update Seller Information

Updates the commercial identity of an existing seller: legal business name, tax identification, and trade name.

**Performed by:** Administrator. DOMINIO 3 assigns the Administrator the "incorporación y mantenimiento" of sellers.

---

# Buyer Management Services

**Source:** OBJ-03; DOMINIO 2. Gestión de Compradores; Sección 3.1 ("Registro de compradores"); Sección 11.

## Register Buyer

Creates a new buyer: a system user with role `BUYER`, together with their primary delivery address, optional additional addresses, and an initial commercial status of `ENABLED`.

Every new buyer starts with an empty active `Cart`, since a buyer holds exactly one active cart at any given time.

The identity document and the email address must be unique across the platform.

**Performed by:** The prospective buyer, without prior authentication. Sección 3.1 includes "Registro de compradores" as a process, and DOMINIO 3 forbids self-registration only for sellers, which implies that buyers do register themselves.

## Consult Buyer Profile

Retrieves the profile of a buyer: addresses and commercial status.

**Performed by:** The buyer themself; Administrator and Supervisor over any buyer. A buyer never accesses the information of another buyer (DOMINIO 2).

## Update Buyer Addresses

Updates the primary address and the additional addresses of a buyer.

Existing orders are never affected, because each order holds a copy of the address chosen at checkout.

**Performed by:** The buyer themself.

## Change Buyer Commercial Status

Changes the commercial status of a buyer between `ENABLED` and `RESTRICTED`.

A `RESTRICTED` buyer keeps access to the platform but cannot place orders.

**Performed by:** Administrator. Inferred: the specification defines the attribute but not who manages it; a buyer cannot restrict themself, and the Administrator is the only administrative profile of Sección 5.

---

# Warehouse Management Services

**Source:** OBJ-04; DOMINIO 4. Gestión de Bodegas; Sección 5 ("Administrador: Responsable de la administración de vendedores y bodegas").

## Register Marketplace Warehouse

Creates a warehouse owned and operated by NexusMarket itself.

**Performed by:** Administrator.

## Register Seller Warehouse

Creates an additional warehouse owned by an existing seller. The seller's first warehouse is created by **Register Seller**.

**Performed by:** Administrator.

## Consult Warehouse

Retrieves the information of a warehouse.

**Performed by:** Administrator, Logistics Operator, and Supervisor over any warehouse; a Seller over their own warehouses.

## Update Warehouse

Updates the information of an existing warehouse, such as its address.

**Performed by:** Administrator.

---

# Catalog Management Services

**Source:** OBJ-05; DOMINIO 5. Gestión del Catálogo; Sección 5 ("Vendedor: Responsable de registrar y administrar sus productos"); Sección 6.1 steps 2 and 4; Matriz de Responsabilidades ("Registro Productos → Vendedor").

## Register Product

Registers a new physical or digital product in the catalog, together with at least one variant.

A product with no real variation is registered with a single default variant. Every product is registered in `DRAFT` and is not visible in the public catalog until it is published, because Sección 6.1 registers the product (step 2) and its inventory (step 3) before publishing it (step 4). `DRAFT` is an inferred value; see `ProductStatus` in *Domain Value Objects*.

**Performed by:** Seller, over their own products.

## Update Product

Updates the commercial information of a product — name, description, and price — and adds new variants to it.

Price changes never alter existing orders, because each order line holds the price frozen at checkout. A discontinued product can no longer be updated.

**Performed by:** Seller, over their own products.

## Publish Product

Makes a product visible in the public catalog by changing its status to `PUBLISHED`.

A draft product becomes visible for the first time through this service, and a suspended product returns to the catalog through it. A physical product can only be published when every one of its variants has inventory registered in at least one warehouse.

**Performed by:** Seller, over their own products.

## Suspend Product

Temporarily removes a published product from the public catalog by changing its status to `SUSPENDED`. The suspension is reversible.

**Performed by:** Seller, over their own products.

## Discontinue Product

Permanently removes a product from active sale by changing its status to `DISCONTINUED`. A draft product that will never be sold can be discontinued directly. The discontinuation is terminal.

**Performed by:** Seller, over their own products.

## Consult Catalog

Retrieves the products visible in the public catalog, that is, those whose status is `PUBLISHED`.

**Performed by:** Any authenticated user.

## Consult Product

Retrieves the full information of a product and its variants.

**Performed by:** The owning Seller over their products in any status; any other authenticated user over published products only.

---

# Inventory Management Services

**Source:** OBJ-06; DOMINIO 6. Gestión del Inventario; Sección 11 ("No se puede reservar inventario inexistente o marcado como 'Dañado'"); Matriz de Responsabilidades ("Administración Inventario → Vendedor, Op. Logístico").

Every change to the stored quantities is recorded as an `InventoryMovement`, and no service may leave a quantity negative under any circumstance.

## Register Inventory Inbound

Registers stock entering a warehouse for a variant of a physical product, and creates the inventory record for that variant and warehouse the first time stock enters it.

Variants of digital products never have inventory.

**Performed by:** Seller, over the inventory of their own products; Logistics Operator.

## Adjust Inventory

Applies a manual correction to the available quantity of an inventory record.

Corrections never edit previous movements; each correction is a new movement of type `ADJUSTMENT`.

**Performed by:** Seller, over the inventory of their own products; Logistics Operator.

## Change Inventory Status

Marks an inventory record as `DAMAGED` or returns it to `AVAILABLE`. Damaged stock cannot be reserved.

**Performed by:** Seller, over the inventory of their own products; Logistics Operator.

## Reserve Inventory

Reserves the stock required by each physical line of a newly placed order, moving units from the available quantity to the reserved quantity.

Each line is reserved in full from a single inventory record with enough stock, which the line keeps as its `sourceInventory`. Stock that does not exist, that is insufficient, or that is marked as `DAMAGED` cannot be reserved; if no single record can cover a line, the order cannot be placed.

**Performed by:** Internal — triggered by **Place Order**.

## Release Inventory Reservation

Returns the units reserved by an order to the available quantity of each line's `sourceInventory` when that order is cancelled, recording a movement of type `RESERVATION_RELEASE`.

**Performed by:** Internal — triggered by **Cancel Order**.

## Register Sale Outbound

Removes from the reserved quantity of each line's `sourceInventory` the units that physically leave the warehouse when a shipment is dispatched.

**Performed by:** Internal — triggered by **Dispatch Shipment**.

## Register Inventory Return

Returns to the available quantity of each returned line's `sourceInventory` the units of a physical product received back from a buyer, so they go back to the warehouse they were sold from.

**Performed by:** Internal — triggered by **Complete Return**.

## Consult Inventory

Retrieves the current available and reserved quantities of a variant, per warehouse.

**Performed by:** Seller, over the inventory of their own products; Logistics Operator, Administrator, and Supervisor over any inventory.

## Consult Inventory Movements

Retrieves the history of movements applied to an inventory record.

**Performed by:** The same participants as **Consult Inventory**.

---

# Cart Management Services

**Source:** OBJ-07; DOMINIO 7 (stage "Carrito"); Sección 6.1 step 5.

A cart carries no commercial commitment and reserves no inventory. Its confirmation is performed by **Place Order**.

## Add Item to Cart

Adds a variant of a published product to the buyer's active cart. Adding a variant already present increases the quantity of the existing line.

**Performed by:** Buyer, over their own cart.

## Update Cart Item Quantity

Changes the quantity of a line already present in the cart. The quantity must remain greater than zero.

**Performed by:** Buyer, over their own cart.

## Remove Item from Cart

Removes a line from the buyer's active cart.

**Performed by:** Buyer, over their own cart.

## Consult Cart

Retrieves the buyer's active cart, valued at the current catalog prices.

**Performed by:** Buyer, over their own cart.

---

# Order Management Services

**Source:** OBJ-08; DOMINIO 7. Gestión de Pedidos; Sección 6.1 steps 5–8; Sección 11 ("Un pedido finalizado no podrá ser modificado bajo ninguna circunstancia"); Matriz de Responsabilidades ("Gestión de Pedidos → Comprador, Vendedor, Op. Logístico").

## Place Order

Confirms the buyer's active cart and converts it into an order in state `PENDING_PAYMENT`.

The service freezes the unit price of each line and the chosen delivery address, reserves inventory for every physical line, closes the confirmed cart, and opens a new empty cart for the buyer.

Only a buyer whose commercial status is `ENABLED` may place an order, the cart must not be empty, and every variant must still belong to a published product.

**Performed by:** Buyer.

## Consult Order

Retrieves an order with its lines, payments, invoice, and shipments.

**Performed by:** The Buyer who placed it; a Seller over the orders that contain their products; Logistics Operator, Administrator, and Supervisor over any order.

## Consult Orders

Retrieves a list of orders filtered by buyer, by seller, or by status, according to the permissions of the requesting user.

**Performed by:** The same participants as **Consult Order**, each restricted to the orders they may access.

## Cancel Order

Cancels an order in state `PENDING_PAYMENT`, releasing every inventory reservation it holds.

An order can no longer be cancelled once paid: the buyer's money has been collected, and the only reimbursement path the specification describes is a refund originated by a return (OBJ-11).

**Performed by:** The Buyer who placed it. Inferred: the specification does not describe cancellation; `CANCELLED` itself is an inferred state (see `OrderStatus` in *Domain Value Objects*), and the buyer is the party of the commercial commitment.

## Confirm Order Payment

Moves an order from `PENDING_PAYMENT` to `PAID` when one of its payment attempts is approved, and triggers the issuance of its invoice.

An order composed exclusively of digital lines moves directly to `DELIVERED`, since digital products are delivered immediately after payment.

**Performed by:** Internal — triggered by **Register Payment** when the attempt is approved.

## Mark Order Dispatched

Moves an order from `PAID` to `DISPATCHED` when its first shipment leaves the warehouse.

**Performed by:** Internal — triggered by **Dispatch Shipment**.

## Mark Order Delivered

Moves an order to `DELIVERED` when every one of its shipments has been delivered. From that moment the order can no longer be modified.

**Performed by:** Internal — triggered by **Confirm Shipment Delivery**.

---

# Payment and Billing Management Services

**Source:** OBJ-09; Sección 4.1 ("Facturación: Información comercial asociada a las ventas"); DOMINIO 7 ("Pendiente de Pago: Espera de confirmación financiera"); Sección 6.1 step 6 ("Se valida el pago").

## Register Payment

Registers a payment attempt for an order in state `PENDING_PAYMENT`, for the full amount of the order, and submits it for financial validation.

The outcome of the validation resolves the attempt as `APPROVED`, `REJECTED`, or `FAILED`. An approved attempt confirms the payment of the order; a rejected or failed attempt leaves the order unchanged and remains as a historical record, and the buyer may register a new attempt.

**Performed by:** Buyer, over their own orders.

**Inferred:** the financial validation is performed by an external party reached through an Output Port. The specification speaks of a "confirmación financiera" but assigns it to no participant of Sección 5, and none of them has financial responsibilities.

## Consult Payments

Retrieves the payment attempts registered for an order.

**Performed by:** The Buyer who placed the order; Administrator and Supervisor over any order.

## Issue Invoice

Issues the invoice of an order when it reaches `PAID`, taking its total amount and currency from the order. An order has at most one invoice, and an issued invoice is immutable.

**Performed by:** Internal — triggered by **Confirm Order Payment**.

## Consult Invoice

Retrieves the invoice of an order.

**Performed by:** The Buyer who was billed; Administrator and Supervisor over any invoice.

---

# Shipment Management Services

**Source:** OBJ-10; Sección 4.1 ("Envíos: Procesos logísticos para productos físicos"); Sección 5 ("Operador Logístico: Encargado de la operación física de bodegas y despachos"); Sección 6.1 steps 7–8.

## Create Shipment

Creates a shipment for a paid order, grouping the physical lines whose `sourceInventory` belongs to one origin warehouse. Every physical line of an order belongs to exactly one shipment, and digital lines never belong to any.

**Performed by:** Logistics Operator.

## Dispatch Shipment

Marks a shipment as having left the warehouse, changing its status from `PENDING` to `IN_TRANSIT`.

Dispatching registers the sale outbound of the corresponding stock and, for the first shipment of an order, moves the order to `DISPATCHED`.

**Performed by:** Logistics Operator.

## Confirm Shipment Delivery

Confirms that a shipment was delivered, changing its status from `IN_TRANSIT` to `DELIVERED`. When the last shipment of an order is delivered, the order moves to `DELIVERED`.

**Performed by:** Logistics Operator.

## Consult Shipments

Retrieves the shipments of an order and their status.

**Performed by:** The Buyer who placed the order; Logistics Operator, Administrator, and Supervisor over any shipment.

---

# Return and Refund Management Services

**Source:** OBJ-11; DOMINIO 6 (movement type "Devolución"); Matriz de Responsabilidades ("Gestión Reembolsos → Comprador, Admin").

## Request Return

Creates a return request over one or more lines of a delivered order, stating the quantity of each line being returned and the reason.

The returned quantity of a line may never exceed the quantity purchased, discounting what was already returned in previous requests.

**Performed by:** The Buyer who placed the order.

## Approve Return

Approves a return request, changing its status to `APPROVED`, and originates the corresponding refund in state `PENDING`, for the sum of the refundable amounts of the returned lines.

**Performed by:** Administrator. Inferred: the Matriz de Responsabilidades assigns the reimbursement process to the Buyer and the Administrator; the Buyer cannot approve their own request, which leaves the Administrator.

## Reject Return

Rejects a return request, changing its status to `REJECTED`. No refund is originated.

**Performed by:** Administrator. Inferred, for the same reason as **Approve Return**.

## Complete Return

Registers that the returned products were received, changing the status of the request from `APPROVED` to `COMPLETED`, and returns the physical units to inventory.

**Performed by:** Logistics Operator. Inferred: receiving goods is part of the "operación física de bodegas" that Sección 5 assigns to this participant.

## Process Refund

Executes a pending refund, changing its status to `PROCESSED` and recording the Administrator who processed it.

**Performed by:** Administrator.

## Reject Refund

Denies a pending refund during administrative review, changing its status to `REJECTED`.

**Performed by:** Administrator.

## Consult Returns and Refunds

Retrieves return requests and their refunds.

**Performed by:** The Buyer over their own requests; Administrator and Supervisor over any request.

---

# Administrative Reporting Services

**Source:** OBJ-12 ("Consolidar información administrativa para consulta"); Sección 3.1 ("Consulta de reportes administrativos"); Sección 5 ("Supervisor: Perfil de consulta y seguimiento operativo").

The specification includes administrative reports as a process but does not enumerate them. The three reports below are **inferred**: each one consolidates one of the business flows that the specification describes, and together they cover the commercial, stock, and after-sales dimensions of the operation. All of them are read-only and generate no operations.

## Consult Sales Report

Consolidates the orders of a period by status, together with their total amounts.

**Performed by:** Supervisor and Administrator.

## Consult Inventory Report

Consolidates the available, reserved, and damaged stock per warehouse and per variant.

**Performed by:** Supervisor and Administrator.

## Consult Returns and Refunds Report

Consolidates the return requests and refunds of a period by status, together with the refunded amounts.

**Performed by:** Supervisor and Administrator.

---

# Operation and Audit Management Services

**Source:** OBJ-12; Sección 1 ("garantizando trazabilidad y coordinación entre todos los participantes"); RG-01. Pattern adopted from the banking reference.

## Register Operation

Creates a business operation representing a significant action performed over a business entity of the marketplace, identified by its `AffectedEntityType` and its identifier.

**Performed by:** Internal — triggered by every service that changes the state of a business entity.

## Register Audit Log

Creates an immutable audit record for a significant business operation, preserving the operation, the user, the role held at that moment, the affected entity, the timestamp, and operation-specific details.

**Performed by:** Internal.

## Register Operation and Audit

Registers an operation and its audit record as a single step. This is the service that the other subdomains actually invoke.

**Performed by:** Internal.

## Consult Operations

Retrieves the operations performed over a business entity or by a user.

**Performed by:** Supervisor and Administrator.

## Consult Audit Log

Retrieves historical audit records according to the access permissions of the requesting user.

**Performed by:** Supervisor and Administrator.

---

# Authorization Services

**Source:** RG-01, RG-02, RG-03; Matriz de Responsabilidades; DOMINIO 2 ("El comprador nunca administrará información de otros compradores ni inventarios").

These services are invoked by every other service before any business rule is applied.

## Validate User Status

Determines whether the requesting user is present and in state `ACTIVE`, and therefore allowed to operate.

## Validate Role Permission

Determines whether the role of the requesting user is allowed to perform a specific business operation.

## Validate Buyer Ownership

Determines whether a buyer is the owner of the cart, order, payment, invoice, or return request being accessed.

## Validate Seller Ownership

Determines whether a seller is the owner of the product, warehouse, or inventory being accessed.

---

# Service-to-Operation Traceability

Every service that changes the state of a business entity produces at least one `Operation`. The table below maps each of them to the `OperationType` and `AffectedEntityType` values defined in *Domain Value Objects*.

| Service                         | OperationType                                          | AffectedEntityType |
| ------------------------------- | ------------------------------------------------------ | ------------------ |
| Register Staff User             | `USER_REGISTRATION`                                    | `USER`             |
| Change User Status              | `USER_STATUS_CHANGE`                                   | `USER`             |
| Register Seller                 | `SELLER_REGISTRATION`, `WAREHOUSE_REGISTRATION`        | `SELLER`, `WAREHOUSE` |
| Update Seller Information       | `SELLER_UPDATE`                                        | `SELLER`           |
| Register Buyer                  | `BUYER_REGISTRATION`                                   | `USER`             |
| Update Buyer Addresses          | `BUYER_PROFILE_UPDATE`                                 | `USER`             |
| Change Buyer Commercial Status  | `BUYER_COMMERCIAL_STATUS_CHANGE`                       | `USER`             |
| Register Marketplace Warehouse  | `WAREHOUSE_REGISTRATION`                               | `WAREHOUSE`        |
| Register Seller Warehouse       | `WAREHOUSE_REGISTRATION`                               | `WAREHOUSE`        |
| Update Warehouse                | `WAREHOUSE_UPDATE`                                     | `WAREHOUSE`        |
| Register Product                | `PRODUCT_REGISTRATION`                                 | `PRODUCT`          |
| Update Product                  | `PRODUCT_UPDATE`                                       | `PRODUCT`          |
| Publish Product                 | `PRODUCT_PUBLICATION`                                  | `PRODUCT`          |
| Suspend Product                 | `PRODUCT_SUSPENSION`                                   | `PRODUCT`          |
| Discontinue Product             | `PRODUCT_DISCONTINUATION`                              | `PRODUCT`          |
| Register Inventory Inbound      | `INVENTORY_INBOUND`                                    | `INVENTORY`        |
| Adjust Inventory                | `INVENTORY_ADJUSTMENT`                                 | `INVENTORY`        |
| Change Inventory Status         | `INVENTORY_STATUS_CHANGE`                              | `INVENTORY`        |
| Reserve Inventory               | `INVENTORY_RESERVATION`                                | `INVENTORY`        |
| Release Inventory Reservation   | `INVENTORY_RESERVATION_RELEASE`                        | `INVENTORY`        |
| Register Sale Outbound          | `INVENTORY_SALE_OUTBOUND`                              | `INVENTORY`        |
| Register Inventory Return       | `INVENTORY_RETURN`                                     | `INVENTORY`        |
| Add Item to Cart                | `CART_ITEM_ADDITION`                                   | `CART`             |
| Update Cart Item Quantity       | `CART_ITEM_UPDATE`                                     | `CART`             |
| Remove Item from Cart           | `CART_ITEM_REMOVAL`                                    | `CART`             |
| Place Order                     | `CART_CONFIRMATION`, `ORDER_PLACEMENT`                 | `CART`, `ORDER`    |
| Cancel Order                    | `ORDER_CANCELLATION`                                   | `ORDER`            |
| Confirm Order Payment           | `ORDER_PAYMENT_CONFIRMATION`                           | `ORDER`            |
| Mark Order Dispatched           | `ORDER_DISPATCH`                                       | `ORDER`            |
| Mark Order Delivered            | `ORDER_DELIVERY`                                       | `ORDER`            |
| Register Payment                | `PAYMENT_REGISTRATION`, then `PAYMENT_APPROVAL`, `PAYMENT_REJECTION`, or `PAYMENT_FAILURE` | `PAYMENT` |
| Issue Invoice                   | `INVOICE_ISSUANCE`                                     | `INVOICE`          |
| Create Shipment                 | `SHIPMENT_CREATION`                                    | `SHIPMENT`         |
| Dispatch Shipment               | `SHIPMENT_DISPATCH`                                    | `SHIPMENT`         |
| Confirm Shipment Delivery       | `SHIPMENT_DELIVERY`                                    | `SHIPMENT`         |
| Request Return                  | `RETURN_REQUEST_CREATION`                              | `RETURN_REQUEST`   |
| Approve Return                  | `RETURN_APPROVAL`                                      | `RETURN_REQUEST`   |
| Reject Return                   | `RETURN_REJECTION`                                     | `RETURN_REQUEST`   |
| Complete Return                 | `RETURN_COMPLETION`                                    | `RETURN_REQUEST`   |
| Process Refund                  | `REFUND_PROCESSING`                                    | `REFUND`           |
| Reject Refund                   | `REFUND_REJECTION`                                     | `REFUND`           |

Every value of `OperationType` defined in *Domain Value Objects* is produced by at least one service of this catalog, and every service in this table has a value. Nine of those values — `SELLER_UPDATE`, `BUYER_PROFILE_UPDATE`, `BUYER_COMMERCIAL_STATUS_CHANGE`, `WAREHOUSE_UPDATE`, `PRODUCT_UPDATE`, `INVENTORY_STATUS_CHANGE`, `INVENTORY_RESERVATION_RELEASE`, `CART_ITEM_UPDATE`, and `PAYMENT_FAILURE` — were added to *Domain Value Objects* when this catalog was defined, because these services change the state of an entity and no existing value described them.

---

# Service Organization

The services described in this document provide the **high-level service catalog** of the system.

They intentionally do not describe implementation details or complete business workflows.

Detailed specifications are maintained in separate Markdown files organized by subdomain:

```text
services/
├── user-authentication-services.md
├── seller-services.md
├── buyer-services.md
├── warehouse-services.md
├── catalog-services.md
├── inventory-services.md
├── cart-services.md
├── order-services.md
├── payment-billing-services.md
├── shipment-services.md
├── return-refund-services.md
├── reporting-services.md
├── operation-audit-services.md
└── authorization-services.md
```
