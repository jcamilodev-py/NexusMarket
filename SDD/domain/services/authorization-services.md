# Authorization Services

## Introduction

This document defines the services responsible for **authorization** within the NexusMarket marketplace platform.

Authorization determines whether an authenticated `User` is allowed to perform a specific business operation according to the user's `SystemRole`, the user's `UserStatus`, and — when the operation concerns information that belongs to someone — the ownership of that information.

Authentication and authorization are separate responsibilities:

```text
Authentication
     │
     ▼
Who is the User?
     │
     ▼
Authorization
     │
     ▼
What can the User do?
```

Authentication is responsible for validating credentials and establishing the identity of the user (`user-authentication-services.md`).

Authorization is responsible for determining whether that authenticated user has permission to execute a requested operation.

The authorization services do not implement the business operation itself. They determine whether the operation may be initiated by the current `User`, and they are invoked by every other service before any business rule is applied.

**Source:** Sección 10 — RG-01 ("Toda operación debe ejecutarse por un usuario autenticado"), RG-02 ("Cada usuario tendrá un único rol dentro del sistema"), RG-03 ("Ningún participante podrá administrar información fuera de su rol"); Sección 5 ("Cada participante desempeña un único rol dentro del sistema y únicamente podrá interactuar con la información correspondiente a sus funciones"); Sección 12. Matriz de Responsabilidades; DOMINIO 2 ("El comprador nunca administrará información de otros compradores ni inventarios").

---

# Domain Model Context

Authorization is based primarily on the following Domain Models:

```text
User
Buyer
Seller
SystemRole
UserStatus
```

The `User` Domain Model contains:

```text
User
├── userId
├── identificationNumber
├── fullName
├── email
├── passwordHash
├── role   : SystemRole
└── status : UserStatus
```

The user's authorization role is represented by:

```text
User.role : SystemRole
```

Ownership in NexusMarket always points to one of two participants:

```text
Buyer    owns    Cart, Order, Payment, Invoice, ReturnRequest
Seller   owns    Product, SellerWarehouse, Inventory of their products
```

Every owned entity already references its owner as a Domain Model (`Order.buyer`, `Product.seller`, `SellerWarehouse.owner`, and so on), so ownership can always be decided by comparing the requesting `User` with that owner — never with a loose identifier.

---

# Authorization Principles

## User as Domain Model

Authorization services must receive a `User` Domain Model.

### Incorrect

```java
authorize(
    String userId,
    String role
);
```

### Correct

```java
execute(User requestingUser, SystemRole... allowedRoles);
```

The role must be obtained from `User.role` and never passed as a `String`.

## Owner as Domain Model

When authorization depends on ownership, the service receives the owner as the Domain Model the calling service already holds.

### Incorrect

```java
validateOwnership(
    String userId,
    String orderId
);
```

### Correct

```java
execute(User requestingUser, Buyer owner);    // owner = order.getBuyer()
execute(User requestingUser, Seller owner);   // owner = product.getSeller()
```

Receiving the owner rather than the owned entity keeps one ownership service per participant type, instead of one per entity: the cart, the order, the invoice, and the return request are all owned by a `Buyer`, and the rule that checks it is the same.

## No Primitive Identifiers

Authorization services must not use primitive identifiers as substitutes for Domain relationships. The same principle applies to every entity of the marketplace.

---

# Authorization Scope

Authorization is divided into two distinct scopes that must not be conflated.

## Read Authorization

Determines whether a user is allowed to **consult** information.

Examples:

```text
A Buyer consults their own order.
A Seller consults the inventory of their own products.
A Supervisor consults any order, the audit log, and the reports.
```

Read authorization depends on the user's role and, for buyers and sellers, on ownership.

## Execute Authorization

Determines whether a user is allowed to **execute or modify** a business operation.

Examples:

```text
An Administrator registers a seller.
A Seller publishes their own product.
A Logistics Operator dispatches a shipment.
```

Execute authorization depends on the user's role, on ownership, and — inside the business service, not here — on the current state of the affected entity.

The **Supervisor** has read authorization over the whole operation and execute authorization over nothing (Sección 5, "Perfil de consulta y seguimiento operativo").

---

# Authorization Responsibilities

Authorization services are responsible for:

* Determining whether the requesting user exists and is `ACTIVE`.
* Determining whether the requesting user holds one of the roles allowed for an operation.
* Validating that a buyer only accesses what belongs to them.
* Validating that a seller only accesses what belongs to them.
* Preventing users from executing operations outside their responsibilities (RG-03).

Authorization services are not responsible for:

* Authenticating credentials.
* Validating passwords.
* Generating or validating tokens.
* Persisting users.
* Executing business operations.
* Validating the state of business entities — whether an order can be cancelled or a product can be published belongs to the business service.
* Implementing REST security filters.

---

# System Roles

Authorization uses the `SystemRole` Value Object. The defined roles are:

```text
BUYER
SELLER
LOGISTICS_OPERATOR
ADMINISTRATOR
SUPERVISOR
```

The authorization services must use the `SystemRole` Value Object instead of raw strings.

---

# Role Authorization Rules

The following rules summarize what each role is authorized to do. The detailed rule of each service lives in its subdomain document; this section is the consolidated view, derived from the **Performed by** of each service in the *Domain Services* catalog.

## BUYER

* May register themself, without prior authentication.
* May consult and update only their own profile and addresses.
* May manage only their own cart, place and cancel only their own orders, and pay only their own orders.
* May consult only their own orders, payments, invoices, shipments, returns, and refunds.
* May request returns only over their own delivered orders.
* May consult the public catalog.
* Never accesses the information of another buyer or any inventory (DOMINIO 2).

## SELLER

* May register, update, publish, suspend, and discontinue only their own products.
* May register inbound stock, adjustments, and status changes only over the inventory of their own products.
* May consult only their own warehouses, inventory, and the orders that contain their products.
* May consult the public catalog.
* Cannot register themself (DOMINIO 3).

## LOGISTICS_OPERATOR

* May register inbound stock, adjustments, and status changes over any inventory (Matriz de Responsabilidades).
* May create, dispatch, and confirm the delivery of shipments.
* May complete returns, receiving the returned products.
* May consult any warehouse, inventory, order, and shipment.

## ADMINISTRATOR

* May register staff users and change the status of any user except themself.
* May register and update sellers, register their warehouses, and register and update marketplace warehouses.
* May change the commercial status of buyers.
* May approve and reject returns, and process and reject refunds.
* May consult any user, buyer, seller, warehouse, inventory, order, payment, invoice, shipment, return, refund, operation, audit record, and report.

## SUPERVISOR

* May consult any user, buyer, seller, warehouse, inventory, order, payment, invoice, shipment, return, refund, operation, audit record, and report.
* Executes no operation that changes the state of the marketplace.

---

# User Status

Authorization must also consider the `UserStatus` of the requesting user.

```text
ACTIVE    →  may operate
INACTIVE  →  may not operate
BLOCKED   →  may not operate
```

The status is always read from the **authoritative** user, loaded through `UserRepositoryPort`, never from the `User` reconstructed from the authentication token. A user blocked after logging in still holds a valid token until it expires; reading the stored status is what stops them immediately (`user-authentication-services.md`, **Token Claims**).

---

# 1. Validate User Status

## Description

Determines whether the requesting user exists and is `ACTIVE`, and returns the authoritative stored `User`.

Every other service calls this one first. From that point on, it works with the returned authoritative user — its role and its status — rather than with the one it received.

**Source:** RG-01; DOMINIO 1 ("Estado: Condición operativa").

---

## Input

```text
requestingUser : User
```

---

## Processing

```text
requestingUser
 │
 ▼
Present? ──no──> Reject
 │
 ▼
UserRepositoryPort.findById
 │
 ▼
Found? ──no──> Reject
 │
 ▼
storedUser.isActive()? ──no──> Reject
 │
 ▼
Authoritative User
```

---

## Result

Returns the authoritative `User`. Every rejection raises `UnauthorizedOperationException`.

A requesting user that no longer exists is rejected as unauthorized rather than as "not found", because the problem is who is acting, not what is being looked for.

---

# 2. Validate Role Permission

## Description

Determines whether the `SystemRole` of a user is one of the roles allowed for an operation.

The service receives the `User` Domain Model and obtains `User.role` from it. It never receives the role independently.

**Source:** RG-02; RG-03; Matriz de Responsabilidades.

---

## Input

```text
user         : User            the authoritative user returned by Validate User Status
allowedRoles : SystemRole...   the roles the calling service accepts
```

---

## Conceptual Processing

```text
User
 │
 ▼
User.role
 │
 ▼
Is it one of allowedRoles?
 │
 ├── yes ──> Authorized
 │
 └── no ───> UnauthorizedOperationException
```

The comparison uses `User.hasRole`.

---

# 3. Validate Buyer Ownership

## Description

Determines whether the requesting user is the `Buyer` who owns the information being accessed: a cart, an order, a payment, an invoice, or a return request.

The calling service passes the owner it already holds, for example `order.getBuyer()`.

**Source:** DOMINIO 2 ("El comprador nunca administrará información de otros compradores ni inventarios"); RG-03.

---

## Input

```text
requestingUser : User
owner          : Buyer
```

---

## Processing

```text
requestingUser + owner
 │
 ▼
requestingUser holds BUYER? ──no──> Reject
 │
 ▼
requestingUser.userId == owner.userId? ──no──> Reject
 │
 ▼
Authorized
```

This service only applies when the requesting user is a buyer. A service that also accepts other roles — for example **Consult Order**, which an Administrator may execute over any order — calls it only when the requesting user holds `BUYER`.

---

# 4. Validate Seller Ownership

## Description

Determines whether the requesting user is the `Seller` who owns the information being accessed: a product, a seller warehouse, or the inventory of their products.

The calling service passes the owner it already holds, for example `product.getSeller()` or `sellerWarehouse.getOwner()`.

**Source:** Sección 5 ("Vendedor: Responsable de registrar y administrar sus productos"); Matriz de Responsabilidades ("Registro Productos → Vendedor"; "Administración Inventario → Vendedor"); RG-03.

---

## Input

```text
requestingUser : User
owner          : Seller
```

---

## Processing

```text
requestingUser + owner
 │
 ▼
requestingUser holds SELLER? ──no──> Reject
 │
 ▼
requestingUser.userId == owner.userId? ──no──> Reject
 │
 ▼
Authorized
```

As with buyer ownership, a service that also accepts other roles — for example **Register Inventory Inbound**, which a Logistics Operator may execute over any inventory — calls it only when the requesting user holds `SELLER`.

---

# Authorization and Business Services

Authorization is performed before executing protected business operations, always in the same order:

```text
Request
   │
   ▼
Input Port
   │
   ▼
Business Service
   │
   ├── 1. Validate User Status ──────> authoritative User
   │
   ├── 2. Validate Role Permission
   │
   ├── 3. Validate Buyer / Seller Ownership   (when the rule depends on it)
   │
   ├── 4. Business rules and state validations
   │
   ├── 5. Persist
   │
   └── 6. Register Operation and Audit
```

The authorization services do not execute the business operation. Any rejection stops the flow before a business rule is applied and before anything is persisted.

---

# Authorization and Authentication

Authentication is responsible for identifying the user. Authorization uses the authenticated `User` Domain Model.

```text
Credentials
    │
    ▼
Login
    │
    ▼
Token ──> (input adapter) ──> User
                                │
                                ▼
                     Authorization Services
                                │
                                ▼
                        Business Service
```

The authorization services must not validate passwords.

---

# Token Relationship

The Domain authorization services operate using the `User` Domain Model. They must not depend directly on:

* JWT libraries.
* HTTP headers.
* Authentication filters.
* Security frameworks.

The conversion from the token to a `User` Domain Model belongs to the input adapter and the security infrastructure:

```text
Token
 │
 ▼
Security Adapter
 │
 ▼
User Domain Model
 │
 ▼
Authorization Service
```

---

# Output Ports

Authorization services use Output Ports only when information outside the received Domain Models is required.

The only port this subdomain uses is:

```text
UserRepositoryPort
```

Ownership never needs a port: the calling service has already loaded the owned entity, and with it its owner.

---

# UserRepositoryPort

## Description

Provides the authoritative stored `User`, whose status and role are the ones authorization trusts.

The contract is defined in `user-authentication-services.md`. This subdomain uses only:

```java
Optional<User> findById(User user);
```

---

# Validation Matrix

| Service                   | Input presence | User status     | Role validation          | Ownership                     | Output Port                 |
| ------------------------- | -------------- | --------------- | ------------------------ | ----------------------------- | --------------------------- |
| Validate User Status      | Yes            | Yes (stored)    | —                        | —                             | `UserRepositoryPort.findById` |
| Validate Role Permission  | Yes            | —               | Yes (one of the allowed) | —                             | —                           |
| Validate Buyer Ownership  | Yes            | —               | Yes (`BUYER`)            | `userId` equals owner's       | —                           |
| Validate Seller Ownership | Yes            | —               | Yes (`SELLER`)           | `userId` equals owner's       | —                           |

## Service-to-Port Matrix

| Service                   | UserRepositoryPort |
| ------------------------- | ------------------ |
| Validate User Status      | `findById`         |
| Validate Role Permission  |                    |
| Validate Buyer Ownership  |                    |
| Validate Seller Ownership |                    |

---

# Input Ports

The authorization services are **internal**: no participant invokes them directly, and no Input Port exposes them. They are injected into the other Domain services, as in the banking reference.

---

# Authorization Flow

## General Flow

```text
Authenticated User
        │
        ▼
Validate User Status ──> UserRepositoryPort
        │
        ▼
Authoritative User
        │
        ▼
Validate Role Permission
        │
        ▼
Validate Buyer / Seller Ownership (when applicable)
        │
        ▼
Authorization Decision
        │
        ├── Authorized
        │       │
        │       ▼
        │   Business Service continues
        │
        └── Unauthorized
                │
                ▼
     UnauthorizedOperationException
```

## Example: a Buyer cancels an order

```text
Buyer
 │
 ▼
Validate User Status        → stored buyer is ACTIVE
 │
 ▼
Validate Role Permission    → BUYER is allowed to cancel
 │
 ▼
Validate Buyer Ownership    → buyer is order.getBuyer()
 │
 ▼
Cancel Order continues      → the order state is validated by Order Management
```

## Example: a Seller or a Logistics Operator registers inbound stock

```text
User
 │
 ▼
Validate User Status
 │
 ▼
Validate Role Permission    → SELLER or LOGISTICS_OPERATOR
 │
 ▼
Holds SELLER? ──yes──> Validate Seller Ownership → seller is inventory's product seller
 │
 no
 │
 ▼
Register Inventory Inbound continues
```

---

# Exceptions

Every rejection of this subdomain raises a single Domain exception:

| Exception                        | Raised when                                                                                      |
| -------------------------------- | ------------------------------------------------------------------------------------------------ |
| `UnauthorizedOperationException` | The requesting user is missing, not stored, not `ACTIVE`, lacks an allowed role, or does not own the information. |

A single exception is deliberate: the caller learns that the operation is not authorized, not which of the conditions failed, so a rejection never reveals whether an entity belongs to someone else.

---

# Business Rules Summary

## BR-AUT-001 — Every operation is executed by an ACTIVE, stored user

The requesting user must exist in the platform and be `ACTIVE`. (RG-01)

## BR-AUT-002 — The stored status and role are authoritative

Authorization never trusts the status or role carried by the token.

## BR-AUT-003 — Each operation accepts a closed set of roles

A user whose single role is not in that set is rejected. (RG-02, RG-03)

## BR-AUT-004 — A buyer only accesses what belongs to them

Carts, orders, payments, invoices, and return requests are accessible to a buyer only when that buyer is their owner. (DOMINIO 2)

## BR-AUT-005 — A seller only accesses what belongs to them

Products, seller warehouses, and the inventory of their products are accessible to a seller only when that seller is their owner. (Sección 5, Matriz de Responsabilidades)

## BR-AUT-006 — The Supervisor never changes state

No service that changes the state of the marketplace accepts the `SUPERVISOR` role. (Sección 5)

## BR-AUT-007 — Authorization precedes every business rule

No business validation or persistence happens before authorization succeeds.

---

# Java Implementation

```text
domain/
├── exceptions/
│   └── UnauthorizedOperationException.java
├── ports/out/
│   └── UserRepositoryPort.java                  (defined in user-authentication-services.md)
└── services/authorization/
    ├── ValidateUserStatusService.java           User execute(User requestingUser)
    ├── ValidateRolePermissionService.java       void execute(User user, SystemRole... allowedRoles)
    ├── ValidateBuyerOwnershipService.java       void execute(User requestingUser, Buyer owner)
    └── ValidateSellerOwnershipService.java      void execute(User requestingUser, Seller owner)
```

Each service is one class with one public method, annotated with `@Service` and `@RequiredArgsConstructor`, as in the banking reference.

---

# Architectural Constraints

The following constraints are mandatory for all Authorization services:

1. Authorization logic belongs exclusively to the Domain layer.
2. Authorization services receive the `User` Domain Model, never a role or an identifier as a `String`.
3. Ownership is validated against the owner Domain Model (`Buyer` or `Seller`) that the calling service already holds.
4. The status and role used are those of the authoritative stored user.
5. Authorization services never validate passwords nor tokens.
6. Authorization services never validate the state of business entities.
7. Authorization services are internal and are not exposed through Input Ports.
8. Every rejection raises `UnauthorizedOperationException`.
9. Authorization services must remain independent of Spring Security, JWT libraries, HTTP, and databases.
10. All authorization rules must remain testable without infrastructure.
