# Buyer Services

## Introduction

This document defines the services belonging to the **Buyer Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Registering new buyers.
- Consulting the profile of a buyer.
- Maintaining the delivery addresses of a buyer.
- Managing the commercial status that determines whether a buyer may purchase.

A buyer is a `User` with role `BUYER`. Authentication, user consultation, and changes to `UserStatus` apply to buyers exactly as to any other user and are defined in `user-authentication-services.md`; this document covers only what is specific to buyers.

**Source:** OBJ-03 ("Administrar compradores registrados"); DOMINIO 2. Gestión de Compradores; Sección 3.1 ("Registro de compradores"); Sección 11 ("El documento de identidad y correo electrónico deben ser únicos en la plataforma").

The services operate exclusively with **Domain Models** and **Value Objects**.

They must never depend directly on databases, persistence entities, SQL, JPA, REST, HTTP, JSON, password hashing libraries, or infrastructure implementations.

Whenever information or functionality external to the Domain is required, the service must communicate through an **Output Port**.

---

# Domain Model Context

`Buyer` is a specialization of the abstract `User`:

```text
User (Abstract)
└── Buyer
    ├── primaryAddress        mandatory
    ├── additionalAddresses   optional, empty by default
    ├── commercialStatus      BuyerCommercialStatus
    ├── activeCart            Cart
    └── orders                loaded on demand
```

The relationship with the cart is represented using the Domain Model:

```text
Buyer.activeCart : Cart
Cart.buyer       : Buyer
```

and never as a loose `String cartId`.

Two independent statuses apply to a buyer:

```text
User.status             UserStatus              platform access — managed in user-authentication-services.md
Buyer.commercialStatus  BuyerCommercialStatus   ability to purchase — managed here
```

A buyer may be an `ACTIVE` user and still be commercially `RESTRICTED`: they can log in, consult their orders, and request returns, but cannot place new orders (DOMINIO 2).

---

# Service Design Principles

## Domain Model Parameters

All services in this subdomain must receive **Domain Models or Value Objects** as parameters.

### Incorrect

```java
registerBuyer(
    String fullName,
    String email,
    String password,
    String primaryAddress
);
```

### Correct

```java
registerBuyer(Buyer newBuyer, Credentials credentials);
```

### Incorrect

```java
updateAddresses(
    String buyerId,
    String primaryAddress,
    List<String> additionalAddresses
);
```

### Correct

```java
updateBuyerAddresses(User requestingUser, Buyer buyer);
```

The new addresses and the new commercial status travel inside the `Buyer` Domain Model, never as separate parameters.

## The Requesting User

Every service of this subdomain except **Register Buyer** receives, as its first parameter, the `User` who is executing it, as established in `user-authentication-services.md`.

**Register Buyer** is the exception because the prospective buyer is not yet a user: registration is what makes them one.

---

# External Information

A service validates directly against the Domain Model whenever the information is available in it — for example, whether the primary address is blank.

Information that depends on other records requires an Output Port — for example, whether the email is already used by any user of the platform, which `UserRepositoryPort` answers.

The service must never access the database directly.

---

# Domain Behavior

The rules that concern a single buyer are enforced by the entities themselves. The services of this subdomain rely on the following behavior:

| Method                                                        | Rule it enforces                                                                                              |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `void User.validateIdentity()`                                | `fullName`, `identificationNumber`, and `email` are present and not blank (DOMINIO 1, Sección 11). Shared with **Register Staff User**. |
| `boolean Buyer.canPlaceOrders()`                              | Only a buyer whose commercial status is `ENABLED` may place orders (DOMINIO 2). Used by `order-services.md`.   |
| `void Buyer.updateAddresses(String primary, List<String> additional)` | The primary address is mandatory and not blank; no additional address is blank; the list is copied, never shared. |
| `void Buyer.changeCommercialStatus(BuyerCommercialStatus newStatus)` | Rejects a missing status and a change to the status already held.                                    |
| `void Buyer.assignActiveCart(Cart cart)`                      | A buyer holds exactly one active cart, which must belong to them.                                             |
| `static Cart Cart.openFor(Buyer buyer)`                       | A new cart belongs to exactly one buyer, starts empty, and is stamped with its creation moment.               |

---

# 1. Register Buyer

## Description

Creates a new buyer: a system user with role `BUYER`, together with their delivery addresses, an initial commercial status of `ENABLED`, and an empty active `Cart`.

**Performed by:** The prospective buyer, without prior authentication. Sección 3.1 includes "Registro de compradores" as a process, and DOMINIO 3 forbids self-registration only for sellers, which implies that buyers do register themselves.

**Source:** OBJ-03; DOMINIO 2; Sección 3.1; Sección 11; RG-02.

---

## Input

```text
newBuyer    : Buyer         identificationNumber, fullName, email, primaryAddress, additionalAddresses
credentials : Credentials   email and password of the new buyer
```

`newBuyer` carries no role, status, commercial status, cart, or password: all of them are established by the service.

---

## Domain Validations

### Buyer Information

* `newBuyer` must be present.
* `User.validateIdentity()`: full name, identification number, and email are not blank.
* `primaryAddress` must not be blank (DOMINIO 2 marks it mandatory).
* No additional address may be blank.

### Credentials

* `credentials` must be present, with neither attribute blank.
* `credentials.email` must be equal to `newBuyer.email` (see `Credentials` in *Domain Value Objects*).

### Uniqueness

The identity document and the email must be unique across **all** users of the platform, not only across buyers (Sección 11):

```text
newBuyer
 │
 ├── identificationNumber ──> UserRepositoryPort.existsByIdentificationNumber
 │
 └── email ─────────────────> UserRepositoryPort.existsByEmail
```

If either already exists, registration is rejected.

---

## Password Processing

The password is hashed through `PasswordServicePort.encode` and assigned with `User.assignPasswordHash`, exactly as in **Register Staff User**. It is never stored in plain text.

---

## Initial State

| Attribute          | Initial value | Reason                                                                                    |
| ------------------ | ------------- | ----------------------------------------------------------------------------------------- |
| `role`             | `BUYER`       | The specialization determines the role (RG-02); the client cannot choose it.              |
| `status`           | `ACTIVE`      | Inferred: a buyer who registers themself does so to operate immediately.                 |
| `commercialStatus` | `ENABLED`     | Inferred: the only reason to restrict a buyer is a commercial decision taken afterwards. |
| `activeCart`       | new empty cart | A buyer holds exactly one active cart at any given time (*Domain Model*, `Buyer`).       |

---

## Processing

```text
newBuyer + credentials
 │
 ▼
Validate buyer information and credentials
 │
 ▼
UserRepositoryPort: email and document uniqueness
 │
 ▼
PasswordServicePort.encode ──> newBuyer.assignPasswordHash
 │
 ▼
Set role BUYER, status ACTIVE, commercialStatus ENABLED
 │
 ▼
BuyerRepositoryPort.save ──> registered buyer (userId assigned)
 │
 ▼
Cart.openFor(registered buyer) ──> CartRepositoryPort.save
 │
 ▼
buyer.assignActiveCart(cart) ──> BuyerRepositoryPort.update
 │
 ▼
Register Operation and Audit (BUYER_REGISTRATION)
```

The buyer is saved before the cart because the cart must reference an existing buyer.

---

## Operation and Audit

| Field                | Value                                   |
| -------------------- | --------------------------------------- |
| `operationType`      | `BUYER_REGISTRATION`                    |
| `affectedEntityType` | `USER`                                  |
| `affectedEntityId`   | `userId` of the registered buyer        |
| `performedBy`        | the registered buyer                    |
| `details`            | none                                    |

The buyer is their own performer: registration is the one operation executed before an authenticated user exists, and it is the buyer who executes it. Opening the initial cart is part of the registration and produces no separate operation.

---

# 2. Consult Buyer Profile

## Description

Retrieves the profile of a buyer: identity, addresses, commercial status, and active cart.

**Performed by:** The buyer themself; Administrator and Supervisor over any buyer. A buyer never accesses the information of another buyer (DOMINIO 2).

**Source:** OBJ-03; DOMINIO 2; RG-03.

---

## Input

```text
requestingUser : User
buyer          : Buyer    carries the userId of the buyer to consult
```

---

## Processing

```text
requestingUser + buyer
 │
 ▼
Validate User Status ──> authoritative requesting user
 │
 ▼
Validate Role Permission (BUYER, ADMINISTRATOR, SUPERVISOR)
 │
 ▼
BuyerRepositoryPort.findById ──> stored buyer? ──no──> EntityNotFoundException
 │
 ▼
Requesting user holds BUYER? ──yes──> Validate Buyer Ownership (stored buyer)
 │
 ▼
Buyer
```

Consultation changes nothing and generates no `Operation`.

---

# 3. Update Buyer Addresses

## Description

Replaces the primary address and the additional addresses of a buyer.

Existing orders are never affected, because each order holds its own copy of the address chosen at checkout (*Domain Model*, `Order.shippingAddress`).

**Performed by:** The buyer themself.

**Source:** DOMINIO 2 ("Dirección principal", "Direcciones adicionales"); OBJ-03; RG-03.

---

## Input

```text
requestingUser : User
buyer          : Buyer    carries the userId and the new primaryAddress and additionalAddresses
```

---

## Processing

```text
requestingUser + buyer
 │
 ▼
Validate User Status
 │
 ▼
Validate Role Permission (BUYER)
 │
 ▼
BuyerRepositoryPort.findById ──> stored buyer
 │
 ▼
Validate Buyer Ownership (stored buyer)
 │
 ▼
storedBuyer.updateAddresses(buyer.primaryAddress, buyer.additionalAddresses)
 │
 ▼
BuyerRepositoryPort.update
 │
 ▼
Register Operation and Audit (BUYER_PROFILE_UPDATE)
```

---

## Operation and Audit

| Field                | Value                                                   |
| -------------------- | ------------------------------------------------------- |
| `operationType`      | `BUYER_PROFILE_UPDATE`                                  |
| `affectedEntityType` | `USER`                                                  |
| `affectedEntityId`   | `userId` of the buyer                                   |
| `performedBy`        | the buyer                                               |
| `details`            | `primaryAddressChanged` (boolean), `additionalAddressCount` |

The details record what changed without copying the addresses themselves into the audit trail: the addresses already live in the buyer's profile and in each order.

---

# 4. Change Buyer Commercial Status

## Description

Changes the commercial status of a buyer between `ENABLED` and `RESTRICTED`.

A `RESTRICTED` buyer keeps access to the platform but cannot place orders. Their `UserStatus` is never modified by this service.

**Performed by:** Administrator. Inferred: the specification defines the attribute but not who manages it; a buyer cannot restrict themself, and the Administrator is the only administrative profile of Sección 5.

**Source:** DOMINIO 2 ("Estado comercial: Condición del comprador para realizar compras"); OBJ-03.

---

## Input

```text
requestingUser : User
buyer          : Buyer    carries the userId and the target commercialStatus
```

---

## Processing

```text
requestingUser + buyer
 │
 ▼
Validate User Status
 │
 ▼
Validate Role Permission (ADMINISTRATOR)
 │
 ▼
BuyerRepositoryPort.findById ──> stored buyer
 │
 ▼
storedBuyer.changeCommercialStatus(buyer.commercialStatus)
 │
 ▼
BuyerRepositoryPort.update
 │
 ▼
Register Operation and Audit (BUYER_COMMERCIAL_STATUS_CHANGE)
```

The specification defines no restriction between `ENABLED` and `RESTRICTED`, so any change to a different value is accepted and a change to the current value is rejected, as with `UserStatus`.

Orders already placed are not affected: the commercial status governs the placement of new orders only.

---

## Operation and Audit

| Field                | Value                                         |
| -------------------- | --------------------------------------------- |
| `operationType`      | `BUYER_COMMERCIAL_STATUS_CHANGE`              |
| `affectedEntityType` | `USER`                                        |
| `affectedEntityId`   | `userId` of the buyer                         |
| `performedBy`        | the Administrator                             |
| `details`            | `previousStatus`, `newStatus`                 |

---

# Output Ports

The ports used by this subdomain are:

```text
BuyerRepositoryPort
CartRepositoryPort
UserRepositoryPort     (defined in user-authentication-services.md)
PasswordServicePort    (defined in user-authentication-services.md)
```

`UserRepositoryPort` answers uniqueness across every user of the platform; `BuyerRepositoryPort` reads and writes the buyer with its buyer-specific attributes, so no service ever has to cast a `User` to a `Buyer`.

---

# BuyerRepositoryPort

## Description

Defines the persistence operations required over buyers. Implemented by the MySQL persistence adapter.

## Contract

```java
public interface BuyerRepositoryPort {

    Buyer save(Buyer buyer);

    Optional<Buyer> findById(Buyer buyer);

    void update(Buyer buyer);
}
```

`save` assigns the `userId`. `update` persists the addresses, the commercial status, and the reference to the active cart.

---

# CartRepositoryPort

## Description

Defines the persistence operations required over carts. This subdomain only opens the initial cart; the rest of the contract is defined in `cart-services.md`.

## Contract used here

```java
Cart save(Cart cart);
```

`save` assigns the cart `identifier`.

---

# Validation Matrix

| Service                         | Requesting user              | Input presence | Role validation                    | Ownership                   | Existence (Output Port)        | Uniqueness (Output Port)        | External processing          |
| ------------------------------- | ---------------------------- | -------------- | ---------------------------------- | --------------------------- | ------------------------------ | ------------------------------- | ---------------------------- |
| Register Buyer                  | — (unauthenticated)          | Yes            | Role set to `BUYER`                | —                           | —                              | Email and identification number | `PasswordServicePort.encode` |
| Consult Buyer Profile           | ACTIVE                       | Yes            | BUYER, ADMINISTRATOR, SUPERVISOR   | When the requester is a buyer | `BuyerRepositoryPort.findById` | —                             | —                            |
| Update Buyer Addresses          | ACTIVE                       | Yes            | BUYER                              | Yes                         | `BuyerRepositoryPort.findById` | —                               | —                            |
| Change Buyer Commercial Status  | ACTIVE                       | Yes            | ADMINISTRATOR                      | —                           | `BuyerRepositoryPort.findById` | —                               | —                            |

## Service-to-Port Matrix

| Service                         | BuyerRepositoryPort          | CartRepositoryPort | UserRepositoryPort                              | PasswordServicePort |
| ------------------------------- | ---------------------------- | ------------------ | ----------------------------------------------- | ------------------- |
| Register Buyer                  | `save`, `update`             | `save`             | `existsByEmail`, `existsByIdentificationNumber` | `encode`            |
| Consult Buyer Profile           | `findById`                   |                    | via Validate User Status                        |                     |
| Update Buyer Addresses          | `findById`, `update`         |                    | via Validate User Status                        |                     |
| Change Buyer Commercial Status  | `findById`, `update`         |                    | via Validate User Status                        |                     |

---

# Transactional Consistency

**Register Buyer** writes three records — the buyer, their cart, and the buyer again with its active cart — followed by the operation and its audit record. They must all succeed or all fail: a buyer without an active cart would violate the rule that every buyer has exactly one. As established in `operation-audit-services.md`, the transaction boundary belongs to the use-case adapter; the Domain guarantees the order and propagates every failure.

---

# Input Ports

| Service                         | Exposed to                                  |
| ------------------------------- | ------------------------------------------- |
| Register Buyer                  | Public access (no authentication)           |
| Consult Buyer Profile           | Buyer, Administrator, Supervisor            |
| Update Buyer Addresses          | Buyer                                       |
| Change Buyer Commercial Status  | Administrator                               |

The Input Ports organized by role are defined in a later phase (`Input-ports.md`).

---

# Authorization Rules

| Service                         | Who may execute it                                  | Source                                     |
| ------------------------------- | --------------------------------------------------- | ------------------------------------------ |
| Register Buyer                  | Anyone, without authentication                      | Sección 3.1; DOMINIO 3 (by contrast)       |
| Consult Buyer Profile           | The buyer themself; Administrator; Supervisor       | DOMINIO 2; RG-03; Sección 5                |
| Update Buyer Addresses          | The buyer themself                                  | DOMINIO 2; RG-03                           |
| Change Buyer Commercial Status  | Administrator                                       | Inferred from DOMINIO 2 and Sección 5      |

---

# Exceptions

| Exception                          | Raised when                                                                                           |
| ---------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `InvalidUserException`             | The buyer to register is missing, or its full name, identification number, or email is blank.         |
| `InvalidBuyerException`            | The primary address is blank, an additional address is blank, no target commercial status was supplied, or a cart that does not belong to the buyer is assigned as active. |
| `UserAlreadyExistsException`       | The email or the identification number is already registered by any user.                             |
| `InvalidCredentialsException`      | Credentials are missing or blank, or their email differs from the buyer's email.                      |
| `InvalidStatusTransitionException` | The target commercial status equals the current one.                                                  |
| `EntityNotFoundException`          | The buyer to consult or to change does not exist.                                                     |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, lacks the required role, or is a buyer accessing another buyer.  |

---

# Business Rules Summary

## BR-BUY-001 — Buyers register themselves

Registration requires no authentication; sellers, by contrast, cannot self-register. (Sección 3.1; DOMINIO 3)

## BR-BUY-002 — Identity document and email are unique across all users

Not only across buyers. (Sección 11)

## BR-BUY-003 — A buyer always has a primary address

It is mandatory at registration and can be replaced but never removed. (DOMINIO 2)

## BR-BUY-004 — A buyer has exactly one active cart

It is opened at registration. (*Domain Model*, `Buyer`)

## BR-BUY-005 — A buyer only accesses their own profile

Administrators and supervisors may consult any buyer. (DOMINIO 2; RG-03)

## BR-BUY-006 — Address changes never alter existing orders

Orders keep the address copied at checkout.

## BR-BUY-007 — Only an ENABLED buyer may place orders

A `RESTRICTED` buyer keeps platform access. (DOMINIO 2)

## BR-BUY-008 — Commercial status and user status are independent

Neither is modified by the services of the other.

## BR-BUY-009 — Only an Administrator changes the commercial status

Inferred from DOMINIO 2 and Sección 5.

---

# Java Implementation

```text
domain/
├── models/
│   ├── User.java                         validateIdentity
│   ├── Buyer.java                        canPlaceOrders, updateAddresses, changeCommercialStatus, assignActiveCart
│   └── Cart.java                         static Cart openFor(Buyer buyer)
├── exceptions/
│   └── InvalidBuyerException.java
├── ports/out/
│   ├── BuyerRepositoryPort.java
│   └── CartRepositoryPort.java
└── services/buyer/
    ├── RegisterBuyerService.java                  Buyer registerBuyer(Buyer newBuyer, Credentials credentials)
    ├── ConsultBuyerProfileService.java            Buyer consultBuyerProfile(User requestingUser, Buyer buyer)
    ├── UpdateBuyerAddressesService.java           Buyer updateBuyerAddresses(User requestingUser, Buyer buyer)
    └── ChangeBuyerCommercialStatusService.java    Buyer changeBuyerCommercialStatus(User requestingUser, Buyer buyer)
```

Each service is one class with one public method, annotated with `@Service` and `@RequiredArgsConstructor`, as in the banking reference.

---

# Architectural Constraints

The following constraints are mandatory for all Buyer services:

1. `Buyer` is a Domain Model that specializes `User`.
2. Services receive Domain Models or Value Objects, never primitive identifiers, isolated attributes, DTOs, or persistence entities.
3. Every service except Register Buyer receives the requesting `User`.
4. The role, status, commercial status, and cart of a new buyer are established by the service, never by the client.
5. Uniqueness is validated against every user of the platform through `UserRepositoryPort`.
6. The password is hashed through `PasswordServicePort` and never stored in plain text.
7. Addresses and commercial status are changed through `Buyer` behavior, never through plain setters.
8. `UserStatus` and `BuyerCommercialStatus` are independent.
9. Every state change registers an `Operation` and its `AuditLog`.
10. External information is always obtained through Output Ports.
11. All buyer rules must remain testable without infrastructure.
