# Seller Services

## Introduction

This document defines the services belonging to the **Seller Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Onboarding new sellers together with their first warehouse.
- Consulting the information of a seller.
- Maintaining the commercial identity of a seller.

A seller is a `User` with role `SELLER`. Authentication, user consultation, and changes to `UserStatus` apply to sellers exactly as to any other user and are defined in `user-authentication-services.md`. Additional warehouses of a seller are registered through `warehouse-services.md`.

**Source:** OBJ-02 ("Gestionar el registro y administración de vendedores"); DOMINIO 3. Gestión de Vendedores ("Los vendedores no pueden auto-registrarse; son incorporados por el Administrador"); Sección 6.1 step 1 ("El Administrador registra al vendedor y su primera bodega"); Matriz de Responsabilidades ("Registro Vendedores → Admin"); Sección 11.

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

`Seller` is a specialization of the abstract `User`:

```text
User (Abstract)
└── Seller
    ├── legalBusinessName   mandatory
    ├── taxId               mandatory
    ├── tradeName           optional
    ├── warehouses          List<SellerWarehouse>, at least one
    └── products            loaded on demand
```

The relationship with its warehouses is represented using Domain Models in both directions:

```text
Seller.warehouses      : List<SellerWarehouse>
SellerWarehouse.owner  : Seller
```

and never as a loose `String sellerId`.

The commercial identity — legal name, tax identification, and trade name — is inferred in the *Domain Model* by analogy with the banking reference's `BusinessCustomer`: a seller sells to third parties and must be identifiable as a fiscal and commercial entity.

---

# Service Design Principles

## Domain Model Parameters

### Incorrect

```java
registerSeller(
    String fullName,
    String email,
    String taxId,
    String warehouseAddress
);
```

### Correct

```java
registerSeller(User requestingUser, Seller newSeller, SellerWarehouse firstWarehouse, Credentials credentials);
```

### Incorrect

```java
updateSeller(
    String sellerId,
    String legalBusinessName,
    String taxId
);
```

### Correct

```java
updateSellerInformation(User requestingUser, Seller seller);
```

The first warehouse is a separate parameter because it is a separate entity with its own identity and its own operation, even though both are created by the same business action.

## The Requesting User

Every service of this subdomain receives, as its first parameter, the `User` who is executing it, as established in `user-authentication-services.md`. Sellers never register themselves, so there is no unauthenticated service here.

---

# Domain Behavior

| Method                                                                   | Rule it enforces                                                                                              |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| `void User.validateIdentity()`                                           | Full name, identification number, and email are not blank. Shared with every registration.                  |
| `void Seller.validateCommercialIdentity()`                               | The legal business name and the tax identification are present and not blank; the trade name is optional.    |
| `List<String> Seller.updateCommercialIdentity(String legalBusinessName, String taxId, String tradeName)` | Applies the same rule to the new values and returns the names of the attributes that actually changed. |
| `void Seller.addWarehouse(SellerWarehouse warehouse)`                    | The warehouse becomes owned by this seller — its `owner` is set here and nowhere else — and is added to the seller's list. |
| `void Warehouse.validateAddress()`                                       | A warehouse always has a non-blank address (`warehouse-services.md`).                                         |

---

# 1. Register Seller

## Description

Onboards a new seller together with their first warehouse, as a single business action.

Creates the seller user with their commercial identity and the first `SellerWarehouse` they own. Neither record can exist without the other: a seller owns at least one warehouse from the moment of onboarding.

**Performed by:** Administrator.

**Source:** DOMINIO 3; Sección 6.1 step 1; Matriz de Responsabilidades ("Registro Vendedores → Admin"); Sección 11; RG-02.

---

## Input

```text
requestingUser : User              the Administrator
newSeller      : Seller            identificationNumber, fullName, email, legalBusinessName, taxId, tradeName
firstWarehouse : SellerWarehouse   address
credentials    : Credentials       email and initial password of the seller
```

`newSeller` carries no role, status, password, or warehouses; `firstWarehouse` carries no owner. All of them are established by the service.

---

## Domain Validations

### Requesting User

`ACTIVE` and `ADMINISTRATOR`, through the **Authorization** services. No other role may register a seller (DOMINIO 3).

### Seller Information

* `newSeller` must be present.
* `User.validateIdentity()`.
* `Seller.validateCommercialIdentity()`: legal business name and tax identification not blank.

### First Warehouse

* `firstWarehouse` must be present.
* `Warehouse.validateAddress()`.

### Credentials

* Present and not blank; `credentials.email` equals `newSeller.email`.

### Uniqueness

Identity document and email must be unique across all users of the platform (Sección 11), through `UserRepositoryPort.existsByIdentificationNumber` and `existsByEmail`.

The tax identification is **not** validated as unique: the specification states uniqueness only for the identity document and the email, and no rule of the marketplace depends on it.

---

## Initial State

| Attribute  | Initial value | Reason                                                                   |
| ---------- | ------------- | ------------------------------------------------------------------------ |
| `role`     | `SELLER`      | The specialization determines the role (RG-02).                          |
| `status`   | `ACTIVE`      | Inferred: the seller is onboarded by an Administrator for immediate use. |
| `warehouses` | the first warehouse | A seller owns at least one warehouse (Sección 6.1 step 1).       |

---

## Processing

```text
requestingUser + newSeller + firstWarehouse + credentials
 │
 ▼
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
Validate seller information, first warehouse, and credentials
 │
 ▼
UserRepositoryPort: email and document uniqueness
 │
 ▼
PasswordServicePort.encode ──> newSeller.assignPasswordHash
 │
 ▼
Set role SELLER, status ACTIVE
 │
 ▼
SellerRepositoryPort.save ──> registered seller (userId assigned)
 │
 ▼
registeredSeller.addWarehouse(firstWarehouse)
 │
 ▼
WarehouseRepositoryPort.save ──> first warehouse (identifier assigned)
 │
 ▼
Register Operation and Audit (SELLER_REGISTRATION)
 │
 ▼
Register Operation and Audit (WAREHOUSE_REGISTRATION)
```

The seller is saved before the warehouse because the warehouse must reference an existing owner.

---

## Operation and Audit

Two operations are registered, one per entity created, so that the warehouse has its own trace from the moment it exists:

| Field                | Seller operation                    | Warehouse operation                          |
| -------------------- | ----------------------------------- | -------------------------------------------- |
| `operationType`      | `SELLER_REGISTRATION`               | `WAREHOUSE_REGISTRATION`                     |
| `affectedEntityType` | `SELLER`                            | `WAREHOUSE`                                  |
| `affectedEntityId`   | `userId` of the seller              | `identifier` of the first warehouse          |
| `performedBy`        | the Administrator                   | the Administrator                            |
| `details`            | `firstWarehouseId`                  | `warehouseType` = `"SELLER"`, `ownerId`      |

---

## Transactional Consistency

The seller, the warehouse, and both operations must all succeed or all fail: a seller without a warehouse would violate Sección 6.1 step 1. The transaction boundary belongs to the use-case adapter, as established in `operation-audit-services.md`.

---

# 2. Consult Seller

## Description

Retrieves the information of a seller, including their commercial identity and their warehouses.

**Performed by:** The seller themself; Administrator and Supervisor over any seller.

**Source:** OBJ-02; RG-03; Sección 5.

---

## Input

```text
requestingUser : User
seller         : Seller   carries the userId of the seller to consult
```

---

## Processing

```text
requestingUser + seller
 │
 ▼
Validate User Status ──> Validate Role Permission (SELLER, ADMINISTRATOR, SUPERVISOR)
 │
 ▼
SellerRepositoryPort.findById ──> stored seller? ──no──> EntityNotFoundException
 │
 ▼
Requesting user holds SELLER? ──yes──> Validate Seller Ownership (stored seller)
 │
 ▼
Seller, with its warehouses
```

Consultation changes nothing and generates no `Operation`.

---

# 3. Update Seller Information

## Description

Updates the commercial identity of an existing seller: legal business name, tax identification, and trade name.

The identity of the user — name, document, email — and the warehouses are not changed by this service.

**Performed by:** Administrator. DOMINIO 3 assigns the Administrator the "incorporación y mantenimiento" of sellers.

**Source:** DOMINIO 3 ("Objetivo: Administrar la incorporación y mantenimiento de proveedores de productos"); OBJ-02.

---

## Input

```text
requestingUser : User
seller         : Seller   carries the userId and the new legalBusinessName, taxId, and tradeName
```

---

## Processing

```text
requestingUser + seller
 │
 ▼
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
SellerRepositoryPort.findById ──> stored seller
 │
 ▼
changedFields = storedSeller.updateCommercialIdentity(seller.legalBusinessName, seller.taxId, seller.tradeName)
 │
 ▼
changedFields empty? ──yes──> InvalidSellerException
 │
 ▼
SellerRepositoryPort.update
 │
 ▼
Register Operation and Audit (SELLER_UPDATE)
```

An update that changes nothing is rejected, for the same reason as a status change to the current status: it would leave a trace of an event that did not happen.

---

## Operation and Audit

| Field                | Value                                                         |
| -------------------- | ------------------------------------------------------------- |
| `operationType`      | `SELLER_UPDATE`                                               |
| `affectedEntityType` | `SELLER`                                                      |
| `affectedEntityId`   | `userId` of the seller                                        |
| `performedBy`        | the Administrator                                             |
| `details`            | `changedFields`: names of the attributes that changed         |

---

# Output Ports

```text
SellerRepositoryPort
WarehouseRepositoryPort   (defined in warehouse-services.md)
UserRepositoryPort        (defined in user-authentication-services.md)
PasswordServicePort       (defined in user-authentication-services.md)
```

---

# SellerRepositoryPort

## Description

Defines the persistence operations required over sellers. Implemented by the MySQL persistence adapter.

## Contract

```java
public interface SellerRepositoryPort {

    Seller save(Seller seller);

    Optional<Seller> findById(Seller seller);

    void update(Seller seller);
}
```

`save` assigns the `userId`. `findById` returns the seller with its warehouses loaded; its products are not loaded, as stated in the *Domain Model*. `update` persists the commercial identity.

---

# Validation Matrix

| Service                   | Requesting user | Role validation                  | Ownership                      | Existence (Output Port)          | Uniqueness (Output Port)        | External processing          |
| ------------------------- | --------------- | -------------------------------- | ------------------------------ | -------------------------------- | ------------------------------- | ---------------------------- |
| Register Seller           | ACTIVE          | ADMINISTRATOR                    | —                              | —                                | Email and identification number | `PasswordServicePort.encode` |
| Consult Seller            | ACTIVE          | SELLER, ADMINISTRATOR, SUPERVISOR | When the requester is a seller | `SellerRepositoryPort.findById` | —                               | —                            |
| Update Seller Information | ACTIVE          | ADMINISTRATOR                    | —                              | `SellerRepositoryPort.findById`  | —                               | —                            |

## Service-to-Port Matrix

| Service                   | SellerRepositoryPort   | WarehouseRepositoryPort | UserRepositoryPort                              | PasswordServicePort |
| ------------------------- | ---------------------- | ----------------------- | ----------------------------------------------- | ------------------- |
| Register Seller           | `save`                 | `save`                  | `existsByEmail`, `existsByIdentificationNumber` | `encode`            |
| Consult Seller            | `findById`             |                         | via Validate User Status                        |                     |
| Update Seller Information | `findById`, `update`   |                         | via Validate User Status                        |                     |

---

# Input Ports

| Service                   | Exposed to                         |
| ------------------------- | ---------------------------------- |
| Register Seller           | Administrator                      |
| Consult Seller            | Seller, Administrator, Supervisor  |
| Update Seller Information | Administrator                      |

---

# Authorization Rules

| Service                   | Who may execute it                             | Source                                                  |
| ------------------------- | ---------------------------------------------- | ------------------------------------------------------- |
| Register Seller           | Administrator                                  | DOMINIO 3; Matriz de Responsabilidades                  |
| Consult Seller            | The seller themself; Administrator; Supervisor | RG-03; Sección 5                                        |
| Update Seller Information | Administrator                                  | DOMINIO 3 ("incorporación y mantenimiento")             |

---

# Exceptions

| Exception                        | Raised when                                                                                         |
| -------------------------------- | --------------------------------------------------------------------------------------------------- |
| `InvalidUserException`           | The seller to register is missing, or its full name, identification number, or email is blank.      |
| `InvalidSellerException`         | The legal business name or the tax identification is blank, or an update changes nothing.           |
| `InvalidWarehouseException`      | The first warehouse is missing or its address is blank.                                             |
| `UserAlreadyExistsException`     | The email or the identification number is already registered by any user.                           |
| `InvalidCredentialsException`    | Credentials are missing or blank, or their email differs from the seller's email.                   |
| `EntityNotFoundException`        | The seller to consult or to update does not exist.                                                  |
| `UnauthorizedOperationException` | The requesting user is not `ACTIVE`, lacks the required role, or is a seller accessing another seller. |

---

# Business Rules Summary

## BR-SEL-001 — Sellers never register themselves

Only an Administrator registers a seller. (DOMINIO 3)

## BR-SEL-002 — A seller is onboarded together with their first warehouse

Both are created by the same business action and neither exists without the other. (Sección 6.1 step 1)

## BR-SEL-003 — A seller owns at least one warehouse at all times

## BR-SEL-004 — Identity document and email are unique across all users

(Sección 11)

## BR-SEL-005 — Legal business name and tax identification are mandatory

Inferred with the commercial identity; see the *Domain Model*.

## BR-SEL-006 — A seller only consults their own information

Administrators and supervisors may consult any seller. (RG-03)

## BR-SEL-007 — Only an Administrator maintains a seller's commercial identity

(DOMINIO 3)

---

# Java Implementation

```text
domain/
├── models/
│   ├── Seller.java                            validateCommercialIdentity, updateCommercialIdentity, addWarehouse
│   └── Warehouse.java                         validateAddress
├── exceptions/
│   ├── InvalidSellerException.java
│   └── InvalidWarehouseException.java
├── ports/out/
│   └── SellerRepositoryPort.java
└── services/seller/
    ├── RegisterSellerService.java              Seller registerSeller(User requestingUser, Seller newSeller, SellerWarehouse firstWarehouse, Credentials credentials)
    ├── ConsultSellerService.java               Seller consultSeller(User requestingUser, Seller seller)
    └── UpdateSellerInformationService.java     Seller updateSellerInformation(User requestingUser, Seller seller)
```

Each service is one class with one public method, annotated with `@Service` and `@RequiredArgsConstructor`, as in the banking reference.

---

# Architectural Constraints

1. `Seller` is a Domain Model that specializes `User`.
2. Services receive Domain Models or Value Objects, never primitive identifiers, isolated attributes, DTOs, or persistence entities.
3. Every service receives the requesting `User`; no seller service runs unauthenticated.
4. Only an Administrator registers and maintains sellers.
5. A seller and their first warehouse are created by the same business action.
6. Warehouse ownership is established only through `Seller.addWarehouse`.
7. Uniqueness is validated against every user of the platform.
8. The password is hashed through `PasswordServicePort` and never stored in plain text.
9. Every state change registers an `Operation` and its `AuditLog`.
10. All seller rules must remain testable without infrastructure.
