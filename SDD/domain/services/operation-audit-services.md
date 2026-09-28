# Operation and Audit Services

## Introduction

This document defines the services responsible for managing **business operations and audit records** within the NexusMarket marketplace platform.

The purpose of this subdomain is to provide traceability for every significant business action performed within the marketplace.

Every significant business action performed over a business entity generates an `Operation`, and every `Operation` is recorded in the `AuditLog`.

Conceptually:

```text
Business entity
      │
      ▼
Business Operation
      │
      ├── Operation   (MySQL)
      │
      └── AuditLog    (MongoDB)
```

This subdomain is responsible for recording and consulting these events. It does not implement the business rules of the entities that originate them.

For example, Order Management decides whether an order can be cancelled. The Operation and Audit services are responsible for registering that the cancellation occurred.

**Source:** OBJ-12 ("Consolidar información administrativa para consulta"); Sección 1 ("garantizando trazabilidad y coordinación entre todos los participantes"); RG-01; Sección 5 (Supervisor as a consultation profile). Pattern adopted from the banking reference `Operation` → `AuditLog` chain.

---

# Domain Model Context

The main Domain Models involved are:

```text
Operation
AuditLog
User
```

Conceptually:

```text
Operation
├── operationId
├── operationType      : OperationType
├── executionDate
├── performedBy        : User
├── affectedEntityType : AffectedEntityType
└── affectedEntityId
```

And:

```text
AuditLog
├── auditId
├── operationType      : OperationType
├── operationDate
├── performedBy        : User
├── userRole           : SystemRole
├── affectedEntityType : AffectedEntityType
├── affectedEntityId
└── details            : Map<String, Object>
```

The relationship with the user is represented using the Domain Model:

```text
Operation.performedBy : User
AuditLog.performedBy  : User
```

and must not be replaced with a primitive `String userId`.

**Difference from the banking reference.** The reference points both models at `BankingProduct`, an abstract root shared by all its products. NexusMarket's affected entities — orders, products, inventory, shipments, refunds, and others — share no common business meaning, so the affected entity is identified by the `AffectedEntityType` catalog plus its identifier (see `Operation` in the *Domain Model*). This is the only place in the model where an identifier stands in for a relationship, and it is always qualified by the catalog.

Persistence adapters are responsible for translating Domain relationships into database representations.

---

# Purpose of Operations

An `Operation` represents a business action executed by the system.

Examples include:

```text
SELLER_REGISTRATION
PRODUCT_PUBLICATION
INVENTORY_RESERVATION
ORDER_PLACEMENT
PAYMENT_APPROVAL
SHIPMENT_DISPATCH
RETURN_APPROVAL
REFUND_PROCESSING
```

The exact list of supported operation types is defined by the `OperationType` Value Object, and the operation produced by each service is listed in the **Service-to-Operation Traceability** table of the *Domain Services* catalog.

The Operation represents the business event independently from its persistence mechanism.

---

# Purpose of Audit Logs

An `AuditLog` represents the historical record of a significant business event.

Audit records provide traceability regarding:

* What operation occurred.
* When it occurred.
* Which user performed it.
* What role the user had at that moment.
* Which business entity was affected.
* Additional operation-specific information.

The audit record is immutable after creation.

```text
Operation
     │
     ▼
AuditLog
     │
     └── Immutable historical record
```

Audit records are persisted in MongoDB, in the `audit_logs` collection of the `nexusmarket_audit` database. Operations are persisted in MySQL together with the business entities.

The Domain communicates with both databases exclusively through Output Ports.

---

# Service Design Principles

## Domain Model Parameters

All Operation and Audit services must receive Domain Models.

They must never receive:

* `String` identifiers.
* Primitive identifiers.
* Individual attributes as substitutes for Domain Models.
* REST DTOs.
* Persistence entities.

### Incorrect

```java
registerOperation(
    String userId,
    String orderId,
    OperationType operationType
);
```

### Correct

```java
execute(Operation operation);
```

The same rule applies to audit records.

### Incorrect

```java
createAudit(
    String userId,
    String orderId,
    OperationType operationType
);
```

### Correct

```java
execute(AuditLog auditLog);
```

---

# Business Responsibility Boundary

The Operation and Audit services must not determine whether a business operation is valid.

```text
Order Management
        │
        ├── Determines whether the order can be cancelled
        │
        ├── Updates the Order and releases its reservations
        │
        ▼
Operation and Audit Services
        │
        ├── Register operation
        │
        └── Register audit
```

The Operation and Audit subdomain is responsible for **traceability**, not for deciding whether the originating business action is allowed.

---

# External Information

The services use the Domain Models received as their primary source of business information.

If information required for the operation or audit process is not available in the Domain Model, the service must use an Output Port.

The service must never access MySQL, MongoDB, SQL, JPA, REST, or infrastructure components directly.

---

# Domain Behavior

The construction rules of both records are enforced by the entities themselves, so that no service can produce an incomplete one:

| Method                                                                                               | Rule it enforces                                                                                                                     |
| ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `static Operation Operation.register(OperationType type, User performedBy, AffectedEntityType entityType, String entityId)` | Rejects a missing type, user, entity type, or entity identifier, and stamps `executionDate` with the current moment. |
| `static AuditLog AuditLog.fromOperation(Operation operation, Map<String, Object> details)`         | Copies the type, date, user, and affected entity from the operation; takes `userRole` from the user at that moment; stores an unmodifiable copy of the details. |

`AuditLog.operationDate` is copied from `Operation.executionDate` rather than taken again, so both records state the same moment for the same event.

---

# 1. Register Operation

## Description

Registers a business operation performed over a business entity of the marketplace.

The operation must contain the Domain information required for traceability.

**Performed by:** Internal — triggered by **Register Operation and Audit**.

---

## Input

```text
Operation
```

The service must not receive individual values such as:

```java
registerOperation(
    String userId,
    String entityId,
    OperationType type
);
```

Instead:

```java
execute(Operation operation);
```

---

## Domain Validations

The service validates the information available in the `Operation` Domain Model:

* The operation is present.
* `operationType` is present.
* `executionDate` is present.
* `performedBy` is present. NexusMarket has no system-triggered operations: every operation is executed by an authenticated user (RG-01), including the internal ones, which inherit the user of the service that triggered them.
* `affectedEntityType` and `affectedEntityId` are present.

---

## User Relationship

The operation contains:

```text
Operation.performedBy : User
```

The service works with the `User` Domain Model; the persistence adapter stores its identifier.

---

## Affected Entity Relationship

The operation contains:

```text
Operation.affectedEntityType : AffectedEntityType
Operation.affectedEntityId   : String
```

The pair always travels together: an identifier without its type is meaningless, since identifiers of different entities are not unique across types.

---

## Persistence

The operation is persisted in MySQL through `OperationRepositoryPort.save`.

---

# 2. Register Audit Log

## Description

Creates an immutable audit record for a significant business event.

**Performed by:** Internal — triggered by **Register Operation and Audit**.

---

## Input

```text
AuditLog
```

### Incorrect

```java
registerAudit(
    String userId,
    String entityId,
    OperationType operationType
);
```

### Correct

```java
execute(AuditLog auditLog);
```

---

## Domain Validations

The service validates:

* The audit record is present.
* `operationType` is present.
* `operationDate` is present.
* `performedBy` is present.
* `userRole` is present.
* `affectedEntityType` and `affectedEntityId` are present.

---

## Immutability

Once an `AuditLog` is persisted, it must not be modified or deleted.

```text
AuditLog
   │
   ▼
Create
   │
   ▼
Persist
   │
   ▼
Immutable historical record
```

`AuditLogRepositoryPort` exposes no update and no delete operation: immutability is enforced by the contract, not only by convention.

---

## Persistence

The audit record is persisted through `AuditLogRepositoryPort.save`. The concrete adapter stores it in MongoDB.

---

# 3. Register Operation and Audit

## Description

Coordinates the registration of a business operation and its corresponding audit record. This is the single traceability registration point that every other subdomain invokes.

**Performed by:** Internal — triggered by every service that changes the state of a business entity.

---

## Input

```text
operation : Operation
details   : Map<String, Object>
```

The operation is built by the calling service with `Operation.register`. The details are the operation-specific information that each subdomain document lists in the **Operation and Audit** table of each service.

---

## Processing

```text
Operation + details
    │
    ▼
Register Operation
    │
    ├── Persist Operation (MySQL)
    │
    └── AuditLog.fromOperation(operation, details)
             │
             ▼
       Register Audit Log
             │
             ▼
       Persist AuditLog (MongoDB)
```

Returns the persisted `Operation`.

---

## Audit Information

The generated audit record includes the information required by the `AuditLog` Domain Model:

```text
operationType
operationDate
performedBy
userRole
affectedEntityType
affectedEntityId
details
```

---

## Transactional Consistency

The state change of the business entity and its `Operation` are both stored in MySQL, so the adapters persist them in the same transaction. The `AuditLog` is stored afterwards in MongoDB; if storing it fails, the failure propagates to the calling service and the MySQL transaction is rolled back, so no business change is ever left without its trace.

The transaction boundary is a concern of the use-case adapters and infrastructure, not of the Domain, as in the banking reference. The Domain guarantees the registration order and propagates every failure.

---

# 4. Consult Operations

## Description

Retrieves recorded operations, either those performed by a user or those that affected a specific business entity.

The returned information is represented using the `Operation` Domain Model. Persistence entities are never exposed.

**Performed by:** Supervisor and Administrator.

---

## Input

The query criterion is represented by a Domain Model, never by a primitive identifier alone:

```text
By performer:        requestingUser : User,  performer : User
By affected entity:  requestingUser : User,  criteria  : Operation   (affectedEntityType + affectedEntityId)
```

---

## Processing

```text
requestingUser + criterion
     │
     ▼
Validate User Status
     │
     ▼
Validate Role Permission (SUPERVISOR, ADMINISTRATOR)
     │
     ▼
OperationRepositoryPort.findByPerformedBy / findByAffectedEntity
     │
     ▼
List<Operation>
```

Consultation changes nothing and generates no `Operation`.

---

# 5. Consult Audit Log

## Description

Retrieves historical audit records by performer, by affected entity, or by operation type.

The returned information is represented by the `AuditLog` Domain Model.

**Performed by:** Supervisor and Administrator.

---

## Input

```text
By performer:        requestingUser : User,  performer : User
By affected entity:  requestingUser : User,  criteria  : AuditLog   (affectedEntityType + affectedEntityId)
By operation type:   requestingUser : User,  criteria  : AuditLog   (operationType)
```

---

## Processing

```text
requestingUser + criterion
     │
     ▼
Validate User Status
     │
     ▼
Validate Role Permission (SUPERVISOR, ADMINISTRATOR)
     │
     ▼
AuditLogRepositoryPort.findByPerformedBy / findByAffectedEntity / findByOperationType
     │
     ▼
List<AuditLog>
```

Consultation changes nothing and generates no `Operation`.

---

# Operation Repository

## Description

The Output Port through which operations are stored in and read from MySQL.

```java
public interface OperationRepositoryPort {

    Operation save(Operation operation);

    List<Operation> findByPerformedBy(User user);

    List<Operation> findByAffectedEntity(Operation criteria);
}
```

It belongs to `domain/ports/out/`. The implementation belongs to the MySQL persistence adapter.

---

# Audit Repository

## Description

The Output Port through which audit records are stored in and read from MongoDB.

```java
public interface AuditLogRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByPerformedBy(User user);

    List<AuditLog> findByAffectedEntity(AuditLog criteria);

    List<AuditLog> findByOperationType(AuditLog criteria);
}
```

The implementation belongs to the MongoDB persistence adapter. It stores the performer and the affected entity as denormalized values, so the audit trail remains readable even if the referenced records change.

---

# User Repository

The Operation and Audit services receive the performing `User` from the calling service, which has already loaded the authoritative user through **Validate User Status**. They therefore need no user lookup of their own; the consultation services use `UserRepositoryPort` only indirectly, through the Authorization services.

---

# Validation Matrix

| Service                      | Input presence | Operation type | User relationship | Affected entity | Role validation          | Operation registration | Audit registration |
| ---------------------------- | -------------- | -------------- | ----------------- | --------------- | ------------------------ | ---------------------- | ------------------ |
| Register Operation           | Yes            | Yes            | Yes               | Yes             | —                        | Registration point     | —                  |
| Register Audit Log           | Yes            | Yes            | Yes, with role    | Yes             | —                        | —                      | Registration point |
| Register Operation and Audit | Yes            | Yes            | Yes               | Yes             | —                        | Yes                    | Yes                |
| Consult Operations           | Yes            | —              | Query criterion   | Query criterion | SUPERVISOR, ADMINISTRATOR | No                    | No                 |
| Consult Audit Log            | Yes            | Query criterion | Query criterion  | Query criterion | SUPERVISOR, ADMINISTRATOR | No                    | No                 |

## Service-to-Port Matrix

| Service                      | OperationRepositoryPort                        | AuditLogRepositoryPort                                         |
| ---------------------------- | ---------------------------------------------- | -------------------------------------------------------------- |
| Register Operation           | `save`                                         |                                                                |
| Register Audit Log           |                                                | `save`                                                         |
| Register Operation and Audit | via Register Operation                         | via Register Audit Log                                         |
| Consult Operations           | `findByPerformedBy`, `findByAffectedEntity`    |                                                                |
| Consult Audit Log            |                                                | `findByPerformedBy`, `findByAffectedEntity`, `findByOperationType` |

The services must not access any of these persistence mechanisms directly.

---

# Input Ports

**Register Operation**, **Register Audit Log**, and **Register Operation and Audit** are internal and are not exposed through Input Ports.

**Consult Operations** and **Consult Audit Log** are exposed to the Supervisor and the Administrator through the Input Ports organized by role, defined in a later phase (`Input-ports.md`).

---

# Business Service Integration

The Operation and Audit services are never invoked independently of the business services that generate the events. For example, a product publication follows:

```text
Publish Product
        │
        ├── Validate authorization
        │
        ├── Validate business rules
        │
        ├── Update Product (PUBLISHED)
        │
        └── Register Operation and Audit
                 │
                 ├── Operation (PRODUCT_PUBLICATION, PRODUCT, productId)
                 │
                 └── AuditLog  (same data + userRole = SELLER + details)
```

The registration always happens **after** the business change succeeds: a rejected action leaves no operation, because nothing happened.

---

# Audit Details

`details` holds the operation-specific information that makes the record useful on its own — for example the previous and new status of a user, the quantity of a stock movement, or the amount of a refund. Each subdomain document lists the details of each of its services.

The following rules apply to every service:

* Values are plain data — text, numbers, dates, and catalog **codes** (`"BLOCKED"`, not the `UserStatus` object) — so the MongoDB document remains readable without the Java model.
* A password, a password hash, or any authentication token is **never** part of the details.
* Details never duplicate the fields the record already has (type, date, user, role, entity).

---

# Audit User Role

`AuditLog.userRole` records the role the user held **when the operation was performed**, not their current role.

In NexusMarket a user's role never changes (BR-USR-001), so both coincide today; the attribute is kept because it is what makes the record self-sufficient — a reader of the audit log never needs to join it with the user table to know in which capacity someone acted.

---

# Audit Immutability

```text
Business change
      │
      ▼
Operation (MySQL) ──> AuditLog (MongoDB)
                            │
                            ▼
                    append-only, never updated, never deleted
```

Corrections are never made by editing a record. If a business action is reverted, the reversal is itself a new business action with its own operation — for example `INVENTORY_ADJUSTMENT` after an incorrect inbound.

---

# Persistence Architecture

```text
                 Domain
                   │
          ┌────────┴─────────┐
          │                  │
 OperationRepositoryPort   AuditLogRepositoryPort
          │                  │
          ▼                  ▼
 MySQL adapter (JPA)      MongoDB adapter (Spring Data MongoDB)
          │                  │
          ▼                  ▼
  nexusmarket_db          nexusmarket_audit.audit_logs
```

---

# Operation Registration Flow

```text
Business Service
      │
      ▼
Operation.register(type, user, entityType, entityId)
      │
      ▼
Register Operation and Audit (operation, details)
      │
      ├── Register Operation ──> OperationRepositoryPort.save
      │
      └── AuditLog.fromOperation ──> Register Audit Log ──> AuditLogRepositoryPort.save
```

---

# Audit Consultation Flow

```text
Supervisor / Administrator
      │
      ▼
Consult Audit Log
      │
      ├── Validate User Status
      │
      ├── Validate Role Permission
      │
      └── AuditLogRepositoryPort
              │
              ▼
        List<AuditLog>
```

---

# Exceptions

| Exception                        | Raised when                                                                         |
| -------------------------------- | ----------------------------------------------------------------------------------- |
| `InvalidOperationException`      | The operation is missing, or lacks its type, date, user, entity type, or entity identifier; or a consultation criterion is missing. |
| `InvalidAuditLogException`       | The audit record is missing, or lacks its type, date, user, role, entity type, or entity identifier; or a consultation criterion is missing. |
| `UnauthorizedOperationException` | The requesting user of a consultation is not `ACTIVE` or is neither Supervisor nor Administrator. |

---

# Business Rules Summary

## BR-OPA-001 — Every state change produces an Operation and an AuditLog

Every service that changes the state of a business entity registers both through **Register Operation and Audit**. (Sección 1, OBJ-12)

## BR-OPA-002 — Registration happens only after the change succeeds

A rejected business action leaves no operation.

## BR-OPA-003 — Every operation has a performing user

There are no anonymous or system operations. (RG-01)

## BR-OPA-004 — The affected entity is always typed

`affectedEntityId` never travels without its `AffectedEntityType`.

## BR-OPA-005 — Audit records are immutable

No update or delete exists for audit records, in the Domain or in the port.

## BR-OPA-006 — The audit role is the role at execution time

`userRole` is taken from the performing user when the record is created.

## BR-OPA-007 — Credentials never reach the audit trail

No password, hash, or token appears in the details.

## BR-OPA-008 — Only Supervisor and Administrator consult the trail

(Sección 5; OBJ-12)

---

# Java Implementation

```text
domain/
├── models/
│   ├── Operation.java                          static Operation register(...)
│   └── AuditLog.java                           static AuditLog fromOperation(...)
├── exceptions/
│   ├── InvalidOperationException.java
│   └── InvalidAuditLogException.java
├── ports/out/
│   ├── OperationRepositoryPort.java
│   └── AuditLogRepositoryPort.java
└── services/operation/
    ├── RegisterOperationService.java           Operation execute(Operation operation)
    ├── RegisterAuditLogService.java            AuditLog execute(AuditLog auditLog)
    ├── RegisterOperationAndAuditService.java   Operation execute(Operation operation, Map<String, Object> details)
    ├── ConsultOperationsService.java           List<Operation> executeByPerformer(User requestingUser, User performer)
    │                                           List<Operation> executeByAffectedEntity(User requestingUser, Operation criteria)
    └── ConsultAuditLogService.java             List<AuditLog> executeByPerformer(User requestingUser, User performer)
                                                List<AuditLog> executeByAffectedEntity(User requestingUser, AuditLog criteria)
                                                List<AuditLog> executeByOperationType(User requestingUser, AuditLog criteria)
```

Each service is annotated with `@Service` and `@RequiredArgsConstructor`. The consultation services expose one public method per query criterion, as the banking reference's `ConsultAuditLogsService` does.

---

# Architectural Constraints

The following constraints are mandatory for all Operation and Audit services:

1. `Operation` and `AuditLog` are Domain Models.
2. Both reference the performing user as a `User` Domain Model.
3. The affected entity is identified by `AffectedEntityType` plus its identifier, never by the identifier alone.
4. Services receive Domain Models, never primitive identifiers, DTOs, or persistence entities.
5. Operations are persisted in MySQL through `OperationRepositoryPort`.
6. Audit records are persisted in MongoDB through `AuditLogRepositoryPort`.
7. The Domain never accesses MySQL or MongoDB directly.
8. Audit records are append-only; no update or delete exists.
9. Operation and audit registration never decide whether a business action is valid.
10. Registration happens after the business change and in the order Operation, then AuditLog.
11. Audit details contain plain values and never credentials.
12. Only the Supervisor and the Administrator consult operations and audit records.
13. All rules must remain testable without infrastructure.
