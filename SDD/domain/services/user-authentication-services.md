# User and Authentication Services

## Introduction

This document defines the services belonging to the **User and Authentication Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Registering the internal users of the marketplace: logistics operators, administrators, and supervisors.
- Authenticating users.
- Consulting user information.
- Managing the operational status of users.

Buyers and sellers are users as well, but they are registered by their own subdomains — **Register Buyer** in `buyer-services.md` and **Register Seller** in `seller-services.md` — because each of those registrations creates business information beyond the user itself: the buyer's addresses and cart, the seller's commercial identity and first warehouse. Once registered, every participant authenticates, is consulted, and changes status through the services of this document.

**Source:** OBJ-01; DOMINIO 1. Administración de Usuarios ("Este dominio constituye la base de autenticación e identificación del Marketplace"); Sección 5. Participantes del Negocio; Sección 10 (RG-01, RG-02, RG-03); Sección 11 ("El documento de identidad y correo electrónico deben ser únicos en la plataforma").

The services operate exclusively with **Domain Models** and **Value Objects**.

They must never depend directly on:

- Databases.
- Persistence entities.
- SQL.
- JPA.
- REST.
- HTTP.
- JSON.
- JWT libraries.
- Password hashing libraries.
- Infrastructure implementations.

Whenever information or functionality external to the Domain is required, the service must communicate through an **Output Port**.

---

# Domain Model Context

`User` is an abstract Domain Model. Every participant of the marketplace is exactly one of its specializations:

```text
User (Abstract)
├── Buyer               registered by buyer-services.md
├── Seller              registered by seller-services.md
├── LogisticsOperator   registered by this document
├── Administrator       registered by this document
└── Supervisor          registered by this document
```

The attributes of `User` relevant to this subdomain are:

```text
User
├── userId                internal identifier
├── identificationNumber  national identity document, unique
├── fullName              not blank
├── email                 unique; the means of access
├── passwordHash          one-way hash; never the plain password
├── role                  SystemRole, exactly one
└── status                UserStatus
```

The role and the specialization always agree: a `LogisticsOperator` holds `LOGISTICS_OPERATOR`, an `Administrator` holds `ADMINISTRATOR`, and so on. This is what RG-02 ("Cada usuario tendrá un único rol dentro del sistema") means in the model.

The password a user types is never a `User` attribute. It travels in the `Credentials` Value Object, which is used only by the services that verify or establish a password (see *Domain Value Objects*).

---

# Service Design Principles

## Domain Model Parameters

All services in this subdomain must receive **Domain Models or Value Objects** as parameters.

They must never receive:

* `String` identifiers.
* Primitive identifiers.
* Individual attributes belonging to a Domain Model.
* Request DTOs.
* Persistence entities.
* Database objects.

### Incorrect

```java
registerStaffUser(
    String fullName,
    String email,
    String password,
    String role
);
```

### Correct

```java
registerStaffUser(User requestingUser, User newUser, Credentials credentials);
```

The same principle applies to authentication.

### Incorrect

```java
login(
    String email,
    String password
);
```

### Correct

```java
login(Credentials credentials);
```

And to status changes.

### Incorrect

```java
changeUserStatus(
    String userId,
    UserStatus status
);
```

### Correct

```java
changeUserStatus(User requestingUser, User user);
```

The target status travels inside the `User` Domain Model (`user.status`), never as a separate parameter.

## The Requesting User

Every service of this subdomain except **Login** receives, as its first parameter, the `User` who is executing it — the authenticated identity reconstructed from the authentication token by the input adapter. This is what makes RG-01 and RG-03 enforceable inside the Domain: a service cannot validate who is acting unless it is told.

**Login** is the only exception, because it is the service that produces the authenticated identity.

---

# External Information

A service must perform validations directly against the Domain Model whenever the required information is already available in it.

If additional information is required from outside the Domain Model, the service must use an Output Port.

For example, email uniqueness cannot be decided from the `User` being registered — it depends on every other user of the platform:

```text
User
 │
 ▼
Register Staff User Service
 │
 ▼
UserRepositoryPort
 │
 ▼
Persistence Adapter
 │
 ▼
MySQL
```

The service must never access the database directly.

---

# Domain Behavior

The rules that concern a single user are enforced by the `User` entity itself, so that they hold regardless of which service touches it. The services of this subdomain rely on the following behavior:

| Method                            | Rule it enforces                                                                                           |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `boolean isActive()`              | Only a user whose status is `ACTIVE` may authenticate or operate (RG-01).                                  |
| `boolean hasRole(SystemRole role)` | Compares the user's single role (RG-02) without exposing the comparison to every caller.                  |
| `void changeStatus(UserStatus newStatus)` | Rejects a missing status and a change to the status the user already holds; otherwise applies it. |
| `void assignPasswordHash(String passwordHash)` | Rejects a blank hash, so a user can never be persisted without one.                           |

Services never assign `status` or `passwordHash` through plain setters; they use these methods.

---

# 1. Register Staff User

## Description

Creates a `User` for one of the internal roles of the marketplace: `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, or `SUPERVISOR`.

The new user is always an instance of the matching specialization — `LogisticsOperator`, `Administrator`, or `Supervisor` — and starts in status `ACTIVE`.

**Performed by:** Administrator. Inferred: OBJ-01 requires administering the information of every user of the marketplace, but the specification only assigns seller registration explicitly (Matriz de Responsabilidades); the Administrator is the only administrative profile of Sección 5.

**Source:** OBJ-01; DOMINIO 1; Sección 5; RG-02; Sección 11.

---

## Input

```text
requestingUser : User          the Administrator executing the service
newUser        : User          LogisticsOperator | Administrator | Supervisor
credentials    : Credentials   email and initial password of the new user
```

`newUser` carries `identificationNumber`, `fullName`, `email`, and `role`. It does not carry a password.

---

## Domain Validations

### Requesting User

The requesting user must be present, `ACTIVE`, and hold the role `ADMINISTRATOR`. The check is delegated to the **Authorization** services (`authorization-services.md`).

### User Information

* `newUser` must be present.
* `fullName` must not be blank (DOMINIO 1, "No vacío").
* `identificationNumber` must not be blank (Sección 11).
* `email` must not be blank (DOMINIO 1).

### Role

* `role` must be `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, or `SUPERVISOR`. `BUYER` and `SELLER` are rejected: those users are registered by their own subdomains.
* `role` must match the specialization of `newUser` (RG-02).

### Credentials

* `credentials` must be present, with neither attribute blank.
* `credentials.email` must be equal to `newUser.email`, since a single address both identifies the participant and gives them access (DOMINIO 1).

### Uniqueness

The identity document and the email must be unique across the platform (Sección 11). This cannot be determined from `newUser` alone, so the service uses `UserRepositoryPort`:

```text
newUser
 │
 ├── identificationNumber ──> UserRepositoryPort.existsByIdentificationNumber ──> already exists?
 │
 └── email ─────────────────> UserRepositoryPort.existsByEmail ─────────────────> already exists?
```

If either already exists, registration is rejected.

---

## Password Processing

The password must never be stored in plain text.

The service obtains the hash through `PasswordServicePort` and assigns it with `User.assignPasswordHash`:

```text
Credentials
 │
 └── password
       │
       ▼
PasswordServicePort.encode
       │
       ▼
passwordHash ──> newUser.assignPasswordHash
```

The Domain must not depend on a concrete implementation such as BCrypt, Argon2, or Spring Security.

---

## Initial State

`newUser.status` is set to `ACTIVE`. Inferred, mirroring the banking reference: the user is created by an Administrator for immediate operational use, so there is no pending state to leave.

`userId` is assigned by the persistence adapter when the user is saved, as in the banking reference.

---

## Persistence

After all validations succeed, the service persists `newUser` through `UserRepositoryPort.save`.

---

## Operation and Audit

| Field                | Value                                   |
| -------------------- | --------------------------------------- |
| `operationType`      | `USER_REGISTRATION`                     |
| `affectedEntityType` | `USER`                                  |
| `affectedEntityId`   | `userId` of the registered user         |
| `performedBy`        | `requestingUser`                        |
| `details`            | `role` of the registered user           |

The password and its hash are never part of the details.

---

# 2. Login

## Description

Authenticates a user through their email and password, and returns the authentication token that represents the authenticated identity for every later request.

The process consists of:

1. Searching for the user by email.
2. Validating the supplied password against the stored hash.
3. Validating that the user's status allows authentication.
4. Generating the authentication token.
5. Returning the token to the caller.

This service defines the business rule of *who* may authenticate. The token mechanism itself is excluded from the specification (Sección 3.2, "mecanismos de autenticación técnica") and is therefore delegated to an Output Port.

**Performed by:** Any registered user, without prior authentication.

**Source:** DOMINIO 1 ("base de autenticación e identificación"; email as "Medio principal de acceso"); RG-01.

---

## Input

```text
credentials : Credentials
```

The service must not receive:

```java
login(String email, String password);
```

It must receive:

```java
login(Credentials credentials);
```

The input adapter converts the external request into `Credentials` before invoking the service.

---

## Login Flow

```text
Credentials
     │
     ▼
Login Service
     │
     ▼
UserRepositoryPort.findByEmail
     │
     ▼
Stored User found?
     │
 ┌───┴────┐
 NO      YES
 │        │
 ▼        ▼
Reject   PasswordServicePort.matches
          │
          ▼
     Password valid?
          │
      ┌───┴────┐
      NO      YES
      │        │
      ▼        ▼
    Reject   User.isActive()?
               │
           ┌───┴────┐
           NO      YES
           │        │
           ▼        ▼
         Reject   JwtServicePort.generateToken
                    │
                    ▼
                  Token
```

---

## Step 1: Search User

The service uses `UserRepositoryPort.findByEmail` to retrieve the stored user whose email matches `credentials.email`.

The service must not directly query MySQL, SQL, JPA, or database repositories. These concerns belong to the Output Adapter.

## Step 2: Validate Password

The service validates the supplied password against `storedUser.passwordHash` through `PasswordServicePort.matches`.

The service must not implement password hashing or comparison itself.

## Step 3: Validate User Status

The service validates the status of the stored user through `User.isActive()`.

```text
ACTIVE    →  authentication allowed
INACTIVE  →  authentication rejected
BLOCKED   →  authentication rejected
```

`BuyerCommercialStatus` is never considered here: a `RESTRICTED` buyer may still authenticate, since commercial restriction only prevents placing orders (DOMINIO 2).

## Step 4: Generate Token

If the user exists, the password is valid, and the user is active, the service requests the token through `JwtServicePort.generateToken`.

The token generation, signing algorithm, secret keys, expiration, and JWT library belong to the adapter or infrastructure layer.

---

## Token Claims

The token must carry enough information for the input adapter to reconstruct the requesting `User` on every later request:

```text
Token
├── userId
├── email
├── fullName
└── role
```

`role` is what determines the specialization to reconstruct.

The password and the password hash must **never** be included in the token.

The token does not carry `status`. A user blocked after logging in must be stopped immediately, not when the token expires, so the **Authorization** services always validate the status of the authoritative user loaded through `UserRepositoryPort` rather than trusting the token (see `authorization-services.md`).

---

## Authentication Result

After successful authentication, the service returns the generated token as a `String`, as in the banking reference.

---

## Failed Authentication

| Situation                        | Result                          |
| -------------------------------- | ------------------------------- |
| No user has that email           | `InvalidCredentialsException`   |
| The password does not match      | `InvalidCredentialsException`   |
| The user is `INACTIVE` or `BLOCKED` | `InvalidUserStatusException` |

An unknown email and a wrong password produce the same exception with the same message, so that a failed login never reveals whether an email is registered. The status is only reported after the password has been verified, for the same reason.

---

## Operation and Audit

Login changes the state of no business entity, so it generates no `Operation`, consistent with the conventions of the *Domain Services* catalog.

---

# 3. Consult User

## Description

Retrieves the information of a system `User` according to the permissions of the requesting user.

The result is returned as a Domain Model. Persistence entities are never returned.

**Performed by:** The user themself over their own information; Administrator and Supervisor over any user.

**Source:** OBJ-01; RG-03; Sección 5 ("Supervisor: Perfil de consulta y seguimiento operativo").

---

## Input

```text
requestingUser : User
user           : User    carries the userId of the user to consult
```

The service must not use `consultUser(String userId)` as a substitute for the Domain Model.

---

## Processing

```text
requestingUser + user
 │
 ▼
Validate requesting user (ACTIVE)
 │
 ▼
Validate access:
   same userId as requestingUser?  ──yes──┐
   requestingUser is ADMINISTRATOR │       │
   or SUPERVISOR?                  ──yes──┤
                                   ──no───> Reject
 │                                          │
 ▼                                          │
UserRepositoryPort.findById  <──────────────┘
 │
 ▼
User found? ──no──> EntityNotFoundException
 │
 ▼
User
```

Access is validated before the lookup, so that a user without permission cannot learn whether another user exists.

---

## Operation and Audit

Consultation changes nothing and generates no `Operation`.

---

# 4. Change User Status

## Description

Changes the `UserStatus` of a system user among `ACTIVE`, `INACTIVE`, and `BLOCKED`.

A user who is not `ACTIVE` can no longer authenticate nor execute any service.

`UserStatus` is independent from `BuyerCommercialStatus`: this service never changes the commercial status of a buyer, and the buyer services never change `UserStatus`.

**Performed by:** Administrator. Inferred, for the same reason as **Register Staff User**.

**Source:** OBJ-01; DOMINIO 1 ("Estado: Condición operativa (Activo, Bloqueado, etc.)").

---

## Input

```text
requestingUser : User
user           : User    carries the userId and the target status
```

---

## Processing

```text
requestingUser + user
 │
 ▼
Validate requesting user (ACTIVE, ADMINISTRATOR)
 │
 ▼
Target status present?
 │
 ▼
UserRepositoryPort.findById ──> stored user
 │
 ▼
requestingUser is the stored user? ──yes──> Reject
 │
 ▼
storedUser.changeStatus(user.status)
 │
 ▼
UserRepositoryPort.update
 │
 ▼
Register Operation and Audit
```

---

## Status Validation

`User.changeStatus` accepts any change to a different value of `UserStatus` and rejects a change to the status the user already holds, because an operation that changes nothing must not leave a trace in the audit log.

The specification defines no transition restrictions among `ACTIVE`, `INACTIVE`, and `BLOCKED`, so none are invented: an administrator may, for example, reactivate a blocked user.

---

## Self-Change Restriction

An Administrator cannot change their own status.

Inferred: a status change is an administrative decision over another participant. Allowing it over oneself would let an Administrator block their own account, and the platform could be left with no active Administrator able to reverse it — every administrative process of the specification (sellers, warehouses, refunds) would stop.

---

## Persistence

The stored user is updated through `UserRepositoryPort.update`.

---

## Operation and Audit

| Field                | Value                                         |
| -------------------- | --------------------------------------------- |
| `operationType`      | `USER_STATUS_CHANGE`                          |
| `affectedEntityType` | `USER`                                        |
| `affectedEntityId`   | `userId` of the affected user                 |
| `performedBy`        | `requestingUser`                              |
| `details`            | `previousStatus`, `newStatus`                 |

---

# Output Ports

All User and Authentication services communicate with external resources exclusively through Output Ports.

The ports used by this subdomain are:

```text
UserRepositoryPort
PasswordServicePort
JwtServicePort
```

They belong to `domain/ports/out/`. Their implementations belong to the adapters.

Operation and audit registration is not a port of this subdomain: the services invoke the **Register Operation and Audit** service of `operation-audit-services.md`, which owns its own ports.

---

# UserRepositoryPort

## Description

Defines the persistence operations required over users. Implemented by the MySQL persistence adapter.

## Contract

```java
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(User user);

    Optional<User> findByEmail(Credentials credentials);

    boolean existsByEmail(User user);

    boolean existsByIdentificationNumber(User user);

    void update(User user);
}
```

The persistence implementation may use the email or the identifier to query the database, but this remains an implementation detail. Every method receives a Domain Model or a Value Object, never a loose identifier.

`findById` and `findByEmail` return the concrete specialization matching the stored role.

---

# PasswordServicePort

## Description

Defines the password security operations required by the Domain.

## Contract

```java
public interface PasswordServicePort {

    String encode(Credentials credentials);

    boolean matches(Credentials credentials, User user);
}
```

* `encode` returns the one-way hash of `credentials.password`.
* `matches` verifies `credentials.password` against `user.passwordHash`.

The implementation may use BCrypt, Argon2, PBKDF2, or another secure hashing mechanism. These technologies must not enter the Domain.

---

# JwtServicePort

## Description

Defines the token operation required by **Login**.

## Contract

```java
public interface JwtServicePort {

    String generateToken(User user);
}
```

The implementation is responsible for creating the token, adding the claims described in **Token Claims**, signing it, and applying its expiration.

The port exposes only Domain types. Unlike the banking reference, whose `JwtServicePort` imports a JWT library type into the Domain, no JWT library type may appear in this interface; validating a token and reconstructing the requesting user from it are input-adapter concerns, performed before the Domain is reached.

---

# Validation Matrix

| Service             | Requesting user                | Input presence | Role validation                         | Existence (Output Port)          | Uniqueness (Output Port)          | External processing                          |
| ------------------- | ------------------------------ | -------------- | --------------------------------------- | -------------------------------- | --------------------------------- | -------------------------------------------- |
| Register Staff User | ACTIVE, ADMINISTRATOR          | Yes            | Staff role matching the specialization  | —                                | Email and identification number   | `PasswordServicePort.encode`                 |
| Login               | — (unauthenticated)            | Yes            | —                                       | `findByEmail`                    | —                                 | `PasswordServicePort.matches`, `JwtServicePort` |
| Consult User        | ACTIVE; self, ADMINISTRATOR, or SUPERVISOR | Yes | —                                    | `findById`                       | —                                 | —                                            |
| Change User Status  | ACTIVE, ADMINISTRATOR, not self | Yes           | —                                       | `findById`                       | —                                 | —                                            |

## Service-to-Port Matrix

| Service             | UserRepositoryPort                                            | PasswordServicePort | JwtServicePort |
| ------------------- | ------------------------------------------------------------- | ------------------- | -------------- |
| Register Staff User | `existsByEmail`, `existsByIdentificationNumber`, `save`       | `encode`            |                |
| Login               | `findByEmail`                                                 | `matches`           | `generateToken` |
| Consult User        | `findById`                                                    |                     |                |
| Change User Status  | `findById`, `update`                                          |                     |                |

---

# Enrichment Pattern

The `User` a service receives is never trusted as authoritative state. When a rule depends on the stored password hash, the stored status, or the stored role, the service first resolves the authoritative `User` through `UserRepositoryPort`, as in the banking reference:

```text
Input User / Credentials
     │
     ▼
UserRepositoryPort
     │
     ▼
Authoritative User
     │
     ▼
Validate domain rules (status / role / ownership)
     │
     ▼
External processing (PasswordServicePort / JwtServicePort)
     │
     ▼
Persist / Return
```

---

# Service and Port Interaction

The dependency direction must always remain:

```text
                 Domain
                   │
          ┌────────┴─────────┐
          │                  │
        Services        Output Ports
                             │
                             ▼
                     Output Adapters
                             │
             ┌───────────────┼───────────────┐
             ▼               ▼               ▼
          MySQL        Password hashing   JWT Provider
```

The services depend on interfaces. They never depend directly on implementations.

---

# Staff User Registration Flow

```text
requestingUser + newUser + credentials
 │
 ▼
Register Staff User
 │
 ├── Validate requesting user (ACTIVE, ADMINISTRATOR)
 │
 ├── Validate user information
 │
 ├── Validate staff role and specialization
 │
 ├── Validate credentials (email matches newUser.email)
 │
 ├── UserRepositoryPort: email and document uniqueness
 │
 ├── PasswordServicePort.encode ──> newUser.assignPasswordHash
 │
 ├── Status ACTIVE
 │
 ├── UserRepositoryPort.save
 │
 └── Register Operation and Audit (USER_REGISTRATION)
```

---

# Input Ports

The services of this subdomain are exposed to the adapters through the Input Ports organized by role, which are defined in a later phase (`Input-ports.md`). The expected mapping is:

| Service             | Exposed to                                     |
| ------------------- | ---------------------------------------------- |
| Login               | Public access (no authentication)              |
| Register Staff User | Administrator                                  |
| Change User Status  | Administrator                                  |
| Consult User        | Every role, restricted as described in service 3 |

The input adapters convert external representations into Domain Models or Value Objects before entering the Domain:

```text
HTTP Request ──> Request DTO ──> Request Mapper ──> Domain Model / Credentials ──> Input Port ──> Domain Service
```

DTOs must never enter the Domain.

---

# Authorization Rules

| Service             | Who may execute it                              | Source                                                   |
| ------------------- | ----------------------------------------------- | -------------------------------------------------------- |
| Register Staff User | Administrator                                   | Inferred from OBJ-01 and Sección 5                       |
| Login               | Anyone, without authentication                  | RG-01 (it is what produces the authenticated user)       |
| Consult User        | The user themself; Administrator; Supervisor    | RG-03; Sección 5                                         |
| Change User Status  | Administrator, never over themself              | Inferred from OBJ-01 and Sección 5                       |

These checks are performed by the services of `authorization-services.md`.

---

# Exceptions

The services of this subdomain raise the following Domain exceptions, all of them subclasses of `DomainException`:

| Exception                         | Raised when                                                                    |
| --------------------------------- | ------------------------------------------------------------------------------ |
| `InvalidUserException`            | A required attribute is missing or blank, or the role does not match the specialization or is not a staff role. |
| `UserAlreadyExistsException`      | The email or the identification number is already registered.                  |
| `InvalidCredentialsException`     | Credentials are missing or blank, the email is unknown, the password does not match, or the credentials' email differs from the new user's email. |
| `InvalidUserStatusException`      | The user attempting to log in is not `ACTIVE`, or no target status was supplied. |
| `InvalidStatusTransitionException` | The target status equals the current status.                                  |
| `EntityNotFoundException`         | The user to consult or to change does not exist.                               |
| `UnauthorizedOperationException`  | The requesting user is missing, not `ACTIVE`, lacks the required role, or tries to change their own status. |

`InvalidStatusTransitionException`, `EntityNotFoundException`, and `UnauthorizedOperationException` are generic and are reused by the other subdomains.

---

# Security Considerations

## Password

Passwords must never be:

* Stored in plain text.
* Returned by any service or API response.
* Included in logs.
* Included in audit records.
* Included in the authentication token.

The password exists only inside `Credentials`, as authentication input, and as the hash stored in `User.passwordHash`.

`passwordHash` itself is also confidential: the REST adapters must never expose it, even though the Domain returns it inside the `User` model.

## Token

The token carries `userId`, `email`, `fullName`, and `role`, and nothing else. Signing and validation remain infrastructure concerns.

---

# Business Rules Summary

## BR-USR-001 — Every user holds exactly one role

The role of a `User` matches its specialization and never changes. (RG-02)

## BR-USR-002 — Identity document and email are unique

No two users share an `identificationNumber` or an `email`. (Sección 11)

## BR-USR-003 — Only staff roles are registered here

Register Staff User creates only `LOGISTICS_OPERATOR`, `ADMINISTRATOR`, and `SUPERVISOR` users; buyers and sellers are registered by their own subdomains. (DOMINIO 2, DOMINIO 3)

## BR-USR-004 — Only an Administrator registers staff and changes statuses

Inferred from OBJ-01 and Sección 5.

## BR-USR-005 — The password is never stored in plain form

Only its hash is stored, obtained through `PasswordServicePort`.

## BR-USR-006 — Only an ACTIVE user authenticates

`INACTIVE` and `BLOCKED` users are rejected at login and by every other service. (RG-01)

## BR-USR-007 — A failed login does not reveal whether an email exists

Unknown email and wrong password produce the same result.

## BR-USR-008 — The requesting user's status is always authoritative

Authorization never relies on the status carried by the token; it reloads the user.

## BR-USR-009 — A status change must change something

Changing a user to the status they already hold is rejected.

## BR-USR-010 — An Administrator cannot change their own status

Inferred; see **Self-Change Restriction**.

## BR-USR-011 — UserStatus and BuyerCommercialStatus are independent

Neither is modified by the services of the other. (DOMINIO 2)

---

# Java Implementation

```text
domain/
├── models/
│   └── User.java                         isActive, hasRole, changeStatus, assignPasswordHash
├── valueobjects/
│   └── Credentials.java
├── exceptions/
│   ├── DomainException.java
│   ├── InvalidUserException.java
│   ├── UserAlreadyExistsException.java
│   ├── InvalidCredentialsException.java
│   ├── InvalidUserStatusException.java
│   ├── InvalidStatusTransitionException.java
│   ├── EntityNotFoundException.java
│   └── UnauthorizedOperationException.java
├── ports/out/
│   ├── UserRepositoryPort.java
│   ├── PasswordServicePort.java
│   └── JwtServicePort.java
└── services/user/
    ├── RegisterStaffUserService.java     User registerStaffUser(User requestingUser, User newUser, Credentials credentials)
    ├── LoginService.java                 String login(Credentials credentials)
    ├── ConsultUserService.java           User consultUser(User requestingUser, User user)
    └── ChangeUserStatusService.java      User changeUserStatus(User requestingUser, User user)
```

Each service is one class with one public method, annotated with `@Service` and `@RequiredArgsConstructor`, as in the banking reference. Dependencies are injected through `final` fields: the Output Ports above, the **Authorization** services, and the **Register Operation and Audit** service.

---

# Architectural Constraints

The following constraints are mandatory for all User and Authentication services:

1. Business logic belongs exclusively to the Domain layer.
2. `User` is an abstract Domain Model; every user is exactly one of its specializations.
3. Services must receive Domain Models or Value Objects as parameters.
4. Services must never receive primitive identifiers as substitutes for Domain relationships.
5. Services must never receive isolated attributes that represent part of a Domain Model.
6. Services must never receive REST Request DTOs or persistence entities.
7. Every service except Login receives the requesting `User`.
8. Services must never access databases directly; external information is always obtained through Output Ports.
9. Output Ports are interfaces owned by the Domain and expose only Domain types.
10. Login searches for the user by the email contained in `Credentials`.
11. Password hashing and verification are performed through `PasswordServicePort`.
12. Passwords are never stored in plain text and never appear in tokens, logs, audit records, or responses.
13. Token generation is performed through `JwtServicePort`; the Domain does not depend on JWT libraries.
14. The token carries `userId`, `email`, `fullName`, and `role`.
15. Authorization always validates the authoritative stored status, never the token.
16. `UserStatus` and `BuyerCommercialStatus` are independent concepts.
17. User status changes are applied through `User.changeStatus`, never through a plain setter.
18. Every state change registers an `Operation` and its `AuditLog` through the Operation and Audit subdomain.
19. All authentication-related business rules must remain testable without infrastructure.
