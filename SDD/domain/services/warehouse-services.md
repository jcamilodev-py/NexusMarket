# Warehouse Services

## Introduction

This document defines the services belonging to the **Warehouse Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Registering warehouses owned by the marketplace.
- Registering additional warehouses owned by a seller.
- Consulting warehouses.
- Maintaining the information of a warehouse.

A seller's **first** warehouse is registered by **Register Seller** (`seller-services.md`), because Sección 6.1 step 1 makes it part of the seller's onboarding. Every other warehouse is registered here.

**Source:** OBJ-04 ("Controlar la información de las bodegas"); DOMINIO 4. Gestión de Bodegas ("Clasificación: Se distinguen bodegas del Marketplace y bodegas de Vendedores"); Sección 5 ("Administrador: Responsable de la administración de vendedores y bodegas"); DOMINIO 6 (inventory is always tied to "una bodega específica").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

`Warehouse` is an abstract Domain Model with two specializations:

```text
Warehouse (Abstract)
├── identifier
├── address
│
├── MarketplaceWarehouse    owned by NexusMarket; no owner attribute
└── SellerWarehouse
    └── owner : Seller
```

The ownership of a seller warehouse is represented using the Domain Model (`SellerWarehouse.owner : Seller`), never as a loose `String sellerId`.

Warehouses are the places where inventory lives (`inventory-services.md`) and from which shipments leave (`shipment-services.md`). A `MarketplaceWarehouse` may hold inventory for variants of any seller; a `SellerWarehouse` holds inventory of its owner's products.

---

# Service Design Principles

## Domain Model Parameters

### Incorrect

```java
registerSellerWarehouse(
    String sellerId,
    String address
);
```

### Correct

```java
registerSellerWarehouse(User requestingUser, SellerWarehouse newWarehouse);
```

The owner travels inside the warehouse (`newWarehouse.owner`), carrying the `userId` of the seller.

### Incorrect

```java
updateWarehouse(
    String warehouseId,
    String address
);
```

### Correct

```java
updateWarehouse(User requestingUser, Warehouse warehouse);
```

## The Requesting User

Every service of this subdomain receives, as its first parameter, the `User` who is executing it.

---

# Domain Behavior

| Method                                          | Rule it enforces                                                                                   |
| ----------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `void Warehouse.validateAddress()`              | A warehouse always has a non-blank address. Inferred in the *Domain Model*: a storage location is meaningless without a physical location. |
| `void Warehouse.relocate(String newAddress)`    | The new address is not blank and differs from the current one.                                    |
| `void Seller.addWarehouse(SellerWarehouse warehouse)` | The warehouse becomes owned by the seller and is added to their list (`seller-services.md`). |

---

# 1. Register Marketplace Warehouse

## Description

Creates a warehouse owned and operated by NexusMarket itself.

**Performed by:** Administrator.

**Source:** DOMINIO 4 ("bodegas del Marketplace"); Sección 5; OBJ-04.

---

## Input

```text
requestingUser : User
newWarehouse   : MarketplaceWarehouse   address
```

---

## Processing

```text
requestingUser + newWarehouse
 │
 ▼
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
newWarehouse present? ──> newWarehouse.validateAddress()
 │
 ▼
WarehouseRepositoryPort.save ──> warehouse (identifier assigned)
 │
 ▼
Register Operation and Audit (WAREHOUSE_REGISTRATION)
```

---

## Operation and Audit

| Field                | Value                                   |
| -------------------- | --------------------------------------- |
| `operationType`      | `WAREHOUSE_REGISTRATION`                |
| `affectedEntityType` | `WAREHOUSE`                             |
| `affectedEntityId`   | `identifier` of the warehouse           |
| `performedBy`        | the Administrator                       |
| `details`            | `warehouseType` = `"MARKETPLACE"`       |

---

# 2. Register Seller Warehouse

## Description

Creates an additional warehouse owned by an existing seller.

**Performed by:** Administrator. Sección 5 assigns the administration of warehouses to the Administrator; the Matriz de Responsabilidades gives the seller no warehouse responsibility.

**Source:** DOMINIO 4 ("bodegas de Vendedores"); Sección 5; OBJ-04.

---

## Input

```text
requestingUser : User
newWarehouse   : SellerWarehouse   address, owner (carrying the seller's userId)
```

---

## Processing

```text
requestingUser + newWarehouse
 │
 ▼
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
newWarehouse present? ──> newWarehouse.validateAddress()
 │
 ▼
SellerRepositoryPort.findById(newWarehouse.owner) ──> stored seller? ──no──> EntityNotFoundException
 │
 ▼
storedSeller.addWarehouse(newWarehouse)
 │
 ▼
WarehouseRepositoryPort.save ──> warehouse (identifier assigned)
 │
 ▼
Register Operation and Audit (WAREHOUSE_REGISTRATION)
```

The owner received is replaced by the stored seller through `Seller.addWarehouse`, so the warehouse is always linked to an existing seller and never to the partial `Seller` sent by the client.

---

## Operation and Audit

| Field                | Value                                            |
| -------------------- | ------------------------------------------------ |
| `operationType`      | `WAREHOUSE_REGISTRATION`                         |
| `affectedEntityType` | `WAREHOUSE`                                      |
| `affectedEntityId`   | `identifier` of the warehouse                    |
| `performedBy`        | the Administrator                                |
| `details`            | `warehouseType` = `"SELLER"`, `ownerId`          |

---

# 3. Consult Warehouse

## Description

Retrieves the information of a warehouse.

**Performed by:** Administrator, Logistics Operator, and Supervisor over any warehouse; a Seller over their own warehouses.

**Source:** OBJ-04; Sección 5 ("Operador Logístico: Encargado de la operación física de bodegas"); RG-03.

---

## Input

```text
requestingUser : User
warehouse      : Warehouse   carries the identifier of the warehouse to consult
```

---

## Processing

```text
requestingUser + warehouse
 │
 ▼
Validate User Status ──> Validate Role Permission (ADMINISTRATOR, LOGISTICS_OPERATOR, SUPERVISOR, SELLER)
 │
 ▼
WarehouseRepositoryPort.findById ──> stored warehouse? ──no──> EntityNotFoundException
 │
 ▼
Requesting user holds SELLER?
 │
 ├── yes ──> stored warehouse is a SellerWarehouse? ──no──> UnauthorizedOperationException
 │                 │
 │                 └── yes ──> Validate Seller Ownership (warehouse.owner)
 │
 ▼
Warehouse
```

A seller cannot consult marketplace warehouses: the catalog restricts sellers to their own warehouses, and a marketplace warehouse has no seller owner.

Consultation changes nothing and generates no `Operation`.

---

# 4. Update Warehouse

## Description

Updates the information of an existing warehouse — its address.

The owner of a seller warehouse never changes: the specification describes no transfer of warehouses between sellers, and every inventory record of that warehouse belongs to the owner's products.

**Performed by:** Administrator.

**Source:** OBJ-04 ("Controlar la información de las bodegas"); Sección 5.

---

## Input

```text
requestingUser : User
warehouse      : Warehouse   carries the identifier and the new address
```

---

## Processing

```text
requestingUser + warehouse
 │
 ▼
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
WarehouseRepositoryPort.findById ──> stored warehouse
 │
 ▼
storedWarehouse.relocate(warehouse.address)
 │
 ▼
WarehouseRepositoryPort.update
 │
 ▼
Register Operation and Audit (WAREHOUSE_UPDATE)
```

---

## Operation and Audit

| Field                | Value                                   |
| -------------------- | --------------------------------------- |
| `operationType`      | `WAREHOUSE_UPDATE`                      |
| `affectedEntityType` | `WAREHOUSE`                             |
| `affectedEntityId`   | `identifier` of the warehouse           |
| `performedBy`        | the Administrator                       |
| `details`            | `previousAddress`, `newAddress`         |

---

# Output Ports

```text
WarehouseRepositoryPort
SellerRepositoryPort      (defined in seller-services.md)
```

---

# WarehouseRepositoryPort

## Description

Defines the persistence operations required over warehouses of both kinds. Implemented by the MySQL persistence adapter.

## Contract

```java
public interface WarehouseRepositoryPort {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(Warehouse warehouse);

    void update(Warehouse warehouse);
}
```

`save` assigns the `identifier`. `findById` returns the concrete specialization — `MarketplaceWarehouse` or `SellerWarehouse` with its owner.

---

# Validation Matrix

| Service                        | Requesting user | Role validation                                             | Ownership                        | Existence (Output Port)                                |
| ------------------------------ | --------------- | ----------------------------------------------------------- | -------------------------------- | ------------------------------------------------------ |
| Register Marketplace Warehouse | ACTIVE          | ADMINISTRATOR                                               | —                                | —                                                      |
| Register Seller Warehouse      | ACTIVE          | ADMINISTRATOR                                               | —                                | `SellerRepositoryPort.findById` (owner)                |
| Consult Warehouse              | ACTIVE          | ADMINISTRATOR, LOGISTICS_OPERATOR, SUPERVISOR, SELLER       | When the requester is a seller   | `WarehouseRepositoryPort.findById`                     |
| Update Warehouse               | ACTIVE          | ADMINISTRATOR                                               | —                                | `WarehouseRepositoryPort.findById`                     |

## Service-to-Port Matrix

| Service                        | WarehouseRepositoryPort | SellerRepositoryPort |
| ------------------------------ | ----------------------- | -------------------- |
| Register Marketplace Warehouse | `save`                  |                      |
| Register Seller Warehouse      | `save`                  | `findById`           |
| Consult Warehouse              | `findById`              |                      |
| Update Warehouse               | `findById`, `update`    |                      |

---

# Input Ports

| Service                        | Exposed to                                                  |
| ------------------------------ | ----------------------------------------------------------- |
| Register Marketplace Warehouse | Administrator                                               |
| Register Seller Warehouse      | Administrator                                               |
| Consult Warehouse              | Administrator, Logistics Operator, Supervisor, Seller       |
| Update Warehouse               | Administrator                                               |

---

# Authorization Rules

| Service                        | Who may execute it                                                     | Source                          |
| ------------------------------ | ---------------------------------------------------------------------- | ------------------------------- |
| Register Marketplace Warehouse | Administrator                                                          | Sección 5; DOMINIO 4            |
| Register Seller Warehouse      | Administrator                                                          | Sección 5; DOMINIO 4            |
| Consult Warehouse              | Administrator, Logistics Operator, Supervisor; a Seller over their own | Sección 5; RG-03                |
| Update Warehouse               | Administrator                                                          | Sección 5; OBJ-04               |

---

# Exceptions

| Exception                          | Raised when                                                                                    |
| ---------------------------------- | ---------------------------------------------------------------------------------------------- |
| `InvalidWarehouseException`        | The warehouse is missing, its address is blank, or the new address equals the current one.     |
| `EntityNotFoundException`          | The warehouse, or the seller who would own it, does not exist.                                 |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, lacks the required role, or is a seller consulting a warehouse they do not own. |

---

# Business Rules Summary

## BR-WHS-001 — Warehouses are either marketplace or seller warehouses

(DOMINIO 4)

## BR-WHS-002 — Every warehouse has an address

Inferred; see the *Domain Model*.

## BR-WHS-003 — A seller warehouse always has an existing owner

Its owner is the stored seller, set through `Seller.addWarehouse`.

## BR-WHS-004 — The owner of a seller warehouse never changes

## BR-WHS-005 — Only an Administrator registers and maintains warehouses

(Sección 5)

## BR-WHS-006 — A seller only consults their own warehouses

(RG-03)

---

# Java Implementation

```text
domain/
├── models/
│   └── Warehouse.java                               validateAddress, relocate
├── exceptions/
│   └── InvalidWarehouseException.java
├── ports/out/
│   └── WarehouseRepositoryPort.java
└── services/warehouse/
    ├── RegisterMarketplaceWarehouseService.java     MarketplaceWarehouse registerMarketplaceWarehouse(User requestingUser, MarketplaceWarehouse newWarehouse)
    ├── RegisterSellerWarehouseService.java          SellerWarehouse registerSellerWarehouse(User requestingUser, SellerWarehouse newWarehouse)
    ├── ConsultWarehouseService.java                 Warehouse consultWarehouse(User requestingUser, Warehouse warehouse)
    └── UpdateWarehouseService.java                  Warehouse updateWarehouse(User requestingUser, Warehouse warehouse)
```

Each service is one class with one public method, annotated with `@Service` and `@RequiredArgsConstructor`, as in the banking reference.

---

# Architectural Constraints

1. `Warehouse` is an abstract Domain Model specialized as `MarketplaceWarehouse` and `SellerWarehouse`.
2. Services receive Domain Models, never primitive identifiers, DTOs, or persistence entities.
3. Every service receives the requesting `User`.
4. A seller's first warehouse is registered by Register Seller; every other warehouse, here.
5. Ownership is established only through `Seller.addWarehouse` and never changes afterwards.
6. Address changes go through `Warehouse.relocate`, never through a plain setter.
7. Every state change registers an `Operation` and its `AuditLog`.
8. All warehouse rules must remain testable without infrastructure.
