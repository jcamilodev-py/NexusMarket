# Return and Refund Services

## Introduction

This document defines the services belonging to the **Return and Refund Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Receiving the buyer's request to return one or more purchased items.
- Approving or rejecting the request, and originating the corresponding refund.
- Receiving the returned products and putting their stock back.
- Executing or denying the refund.
- Consulting returns and refunds.

The specification separates **returns** — the product going back — from **refunds** — the money going back (OBJ-11), and assigns the reimbursement process to the buyer and the administrator (Matriz de Responsabilidades). The model follows that separation: `ReturnRequest` for the first, `Refund` for the second.

**Source:** OBJ-11 ("Administrar devoluciones y reembolsos"); Sección 3.1 ("Gestión de devoluciones"; "Gestión de reembolsos"); DOMINIO 6 (movement type "Devolución"); Matriz de Responsabilidades ("Gestión Reembolsos → Comprador, Admin").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

```text
ReturnRequest
├── identifier
├── order         : Order                DELIVERED
├── requestedBy   : Buyer                the buyer of the order
├── returnItems   : List<ReturnItem>
│                     ├── orderItem          : OrderItem     frozen unitPrice, sourceInventory
│                     ├── quantity
│                     └── refundableAmount   quantity × orderItem.unitPrice
├── reason
├── requestDate
└── returnStatus  : ReturnStatus         REQUESTED ──> APPROVED ──> COMPLETED
                                                   └─> REJECTED

Refund
├── identifier
├── returnRequest : ReturnRequest        APPROVED
├── amount, currency                     Σ refundableAmount, order's currency
├── refundStatus  : RefundStatus         PENDING ──> PROCESSED | REJECTED
├── processedBy   : Administrator
└── processDate
```

The refund always reimburses what the buyer actually paid, because `refundableAmount` is computed from the price frozen in the order line, never from the current catalog price (*Domain Model*, **Frozen Commercial Conditions**).

---

# Service Design Principles

### Incorrect

```java
requestReturn(String orderId, String variantId, int quantity, String reason);
```

### Correct

```java
requestReturn(User requestingUser, ReturnRequest request);
```

The request carries its order, its reason, and its lines; each line identifies the order line by its variant and states the quantity. The amounts are never received: they are computed from the order.

---

# Domain Behavior

| Method                                                                 | Rule it enforces                                                                                           |
| ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `static ReturnRequest ReturnRequest.request(Order order, List<ReturnItem> items, String reason, List<ReturnRequest> previousRequests)` | The order is `DELIVERED`; there is at least one line and a reason; each line refers to a line of the order and returns a positive quantity no greater than purchased minus already requested in previous requests that were not rejected; computes every `refundableAmount`; starts `REQUESTED`. |
| `BigDecimal ReturnRequest.totalRefundableAmount()`                     | Sum of the refundable amounts of the lines.                                                               |
| `void ReturnRequest.approve()`                                         | Only from `REQUESTED`.                                                                                    |
| `void ReturnRequest.reject()`                                          | Only from `REQUESTED`.                                                                                    |
| `void ReturnRequest.complete()`                                        | Only from `APPROVED`.                                                                                     |
| `static Refund Refund.originateFrom(ReturnRequest request)`            | The request is `APPROVED`; amount is its total refundable amount and currency the order's; starts `PENDING`. |
| `void Refund.process(Administrator administrator)`                     | Only from `PENDING`; records who processed it and when.                                                   |
| `void Refund.reject(Administrator administrator)`                      | Only from `PENDING`; records who reviewed it and when.                                                    |

`processedBy` and `processDate` record the administrator who **decided** the refund, whether to execute or to deny it: both are administrative decisions that must be traceable to a person.

---

# 1. Request Return

## Description

Creates a return request over one or more lines of a delivered order, stating the quantity of each line and the reason.

The returned quantity of a line may never exceed the quantity purchased, discounting what was already requested in previous requests that were not rejected (*Domain Model*, `ReturnRequest`). A rejected request frees its quantities again.

**Performed by:** The buyer who placed the order.

**Source:** OBJ-11; Matriz de Responsabilidades; DOMINIO 7 (a return concerns a delivered order).

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
OrderRepositoryPort.findById(request.order) ──> Validate Buyer Ownership
 │
 ▼
ReturnRequestRepositoryPort.findByOrder ──> previous requests
 │
 ▼
newRequest = ReturnRequest.request(order, lines, reason, previousRequests)
 │
 ▼
ReturnRequestRepositoryPort.save
 │
 ▼
Register Operation and Audit (RETURN_REQUEST_CREATION)
```

Details: `orderId`, `itemCount`, `refundableAmount`.

---

# 2. Approve Return

## Description

Approves a return request, changing its status to `APPROVED`, and originates the corresponding refund in state `PENDING`, for the sum of the refundable amounts of the returned lines.

**Performed by:** Administrator. Inferred: the Matriz de Responsabilidades assigns the reimbursement process to the buyer and the administrator; the buyer cannot approve their own request, which leaves the administrator.

**Source:** OBJ-11; Matriz de Responsabilidades; *Domain Value Objects*, `ReturnStatus`.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
ReturnRequestRepositoryPort.findById
 │
 ▼
request.approve() ──> ReturnRequestRepositoryPort.update
 │
 ▼
refund = Refund.originateFrom(request) ──> RefundRepositoryPort.save
 │
 ▼
Register Operation and Audit (RETURN_APPROVAL)
```

Details: `refundId`, `refundAmount`, `currency`.

---

# 3. Reject Return

## Description

Rejects a return request, changing its status to `REJECTED`. No refund is originated.

**Performed by:** Administrator. Inferred, for the same reason as **Approve Return**.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
ReturnRequestRepositoryPort.findById ──> request.reject() ──> update
 │
 ▼
Register Operation and Audit (RETURN_REJECTION)
```

Details: `orderId`.

---

# 4. Complete Return

## Description

Registers that the returned products were received, changing the status of the request from `APPROVED` to `COMPLETED`, and returns the physical units to inventory — into the record each line was sold from (`inventory-services.md`, **Register Inventory Return**).

Digital lines produce no stock movement: digital products never have inventory (DOMINIO 5).

**Performed by:** Logistics Operator. Inferred: receiving goods is part of the "operación física de bodegas" that Sección 5 assigns to this participant.

**Source:** DOMINIO 6 ("Devolución"); Sección 5; *Domain Value Objects*, `ReturnStatus`.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (LOGISTICS_OPERATOR)
 │
 ▼
ReturnRequestRepositoryPort.findById
 │
 ▼
request.complete()
 │
 ▼
For each physical line: Register Inventory Return (operator, line)
 │
 ▼
ReturnRequestRepositoryPort.update
 │
 ▼
Register Operation and Audit (RETURN_COMPLETION)
```

Details: `returnedPhysicalLineCount`.

The refund does not depend on the completion: once the return is approved, the administrator may process the refund before or after the products are received. The specification sets no order between the two, and requiring one would block reimbursements whenever the goods are delayed in transit.

---

# 5. Process Refund

## Description

Executes a pending refund, changing its status to `PROCESSED` and recording the administrator who processed it.

**Performed by:** Administrator.

**Source:** Matriz de Responsabilidades ("Gestión Reembolsos → Admin"); OBJ-11.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
RefundRepositoryPort.findById
 │
 ▼
refund.process(administrator) ──> RefundRepositoryPort.update
 │
 ▼
Register Operation and Audit (REFUND_PROCESSING)
```

Details: `amount`, `currency`, `returnRequestId`.

---

# 6. Reject Refund

## Description

Denies a pending refund during administrative review, changing its status to `REJECTED`.

**Performed by:** Administrator.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (ADMINISTRATOR)
 │
 ▼
RefundRepositoryPort.findById ──> refund.reject(administrator) ──> update
 │
 ▼
Register Operation and Audit (REFUND_REJECTION)
```

Details: `returnRequestId`.

---

# 7. Consult Returns and Refunds

## Description

Retrieves return requests, and the refund originated by a request.

| Requesting role                  | Return requests returned     |
| -------------------------------- | ---------------------------- |
| Buyer                            | Their own                    |
| Administrator, Supervisor        | All                          |

**Performed by:** The buyer over their own requests; Administrator and Supervisor over any request.

---

## Processing

```text
Consult returns:
  Validate User Status ──> Validate Role Permission (BUYER, ADMINISTRATOR, SUPERVISOR)
   │
   ├── BUYER  ──> ReturnRequestRepositoryPort.findByBuyer
   └── others ──> ReturnRequestRepositoryPort.findAll

Consult refund of a request:
  Validate User Status ──> Validate Role Permission
   │
   ▼
  ReturnRequestRepositoryPort.findById ──> buyer ownership when the requester is a buyer
   │
   ▼
  RefundRepositoryPort.findByReturnRequest ──> none? ──> EntityNotFoundException
```

---

# Output Ports

```text
ReturnRequestRepositoryPort
RefundRepositoryPort
OrderRepositoryPort   (order-services.md)
```

## ReturnRequestRepositoryPort

```java
public interface ReturnRequestRepositoryPort {

    ReturnRequest save(ReturnRequest request);

    Optional<ReturnRequest> findById(ReturnRequest request);

    List<ReturnRequest> findByOrder(Order order);

    List<ReturnRequest> findByBuyer(User buyer);

    List<ReturnRequest> findAll();

    void update(ReturnRequest request);
}
```

Lookups return the request with its order, buyer, and lines — each line with its order line, variant, product, and source inventory.

## RefundRepositoryPort

```java
public interface RefundRepositoryPort {

    Refund save(Refund refund);

    Optional<Refund> findById(Refund refund);

    Optional<Refund> findByReturnRequest(ReturnRequest request);

    void update(Refund refund);
}
```

---

# Validation Matrix

| Service                     | Requesting user | Role                              | Ownership         | State rule                                             |
| --------------------------- | --------------- | --------------------------------- | ----------------- | ------------------------------------------------------ |
| Request Return              | ACTIVE          | BUYER                             | Own order         | Order DELIVERED; quantities within what was purchased  |
| Approve Return              | ACTIVE          | ADMINISTRATOR                     | —                 | Request REQUESTED                                      |
| Reject Return               | ACTIVE          | ADMINISTRATOR                     | —                 | Request REQUESTED                                      |
| Complete Return             | ACTIVE          | LOGISTICS_OPERATOR                | —                 | Request APPROVED                                       |
| Process Refund              | ACTIVE          | ADMINISTRATOR                     | —                 | Refund PENDING                                         |
| Reject Refund               | ACTIVE          | ADMINISTRATOR                     | —                 | Refund PENDING                                         |
| Consult Returns and Refunds | ACTIVE          | BUYER, ADMINISTRATOR, SUPERVISOR  | Buyer: own        | —                                                      |

## Service-to-Port Matrix

| Service                     | ReturnRequestRepositoryPort              | RefundRepositoryPort            | OrderRepositoryPort | Internal services invoked   |
| --------------------------- | ---------------------------------------- | ------------------------------- | ------------------- | --------------------------- |
| Request Return              | `findByOrder`, `save`                    |                                 | `findById`          |                             |
| Approve Return              | `findById`, `update`                     | `save`                          |                     |                             |
| Reject Return               | `findById`, `update`                     |                                 |                     |                             |
| Complete Return             | `findById`, `update`                     |                                 |                     | Register Inventory Return   |
| Process Refund              |                                          | `findById`, `update`            |                     |                             |
| Reject Refund               |                                          | `findById`, `update`            |                     |                             |
| Consult Returns and Refunds | `findByBuyer`, `findAll`, `findById`     | `findByReturnRequest`           |                     |                             |

---

# Input Ports

| Service                                                  | Exposed to                         |
| -------------------------------------------------------- | ---------------------------------- |
| Request Return                                           | Buyer                              |
| Approve Return, Reject Return, Process Refund, Reject Refund | Administrator                  |
| Complete Return                                          | Logistics Operator                 |
| Consult Returns and Refunds                              | Buyer, Administrator, Supervisor   |

---

# Exceptions

| Exception                          | Raised when                                                                                         |
| ---------------------------------- | --------------------------------------------------------------------------------------------------- |
| `InvalidReturnException`           | The order is not delivered; the request has no lines or no reason; a line does not belong to the order; a quantity is not positive or exceeds what can still be returned. |
| `InvalidRefundException`           | A refund is originated from a request that is not approved.                                         |
| `InvalidStatusTransitionException` | A return or refund transition is requested from a status that does not allow it.                    |
| `EntityNotFoundException`          | The order, request, or refund does not exist.                                                       |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, lacks the required role, or does not own the order or request. |

---

# Business Rules Summary

## BR-RET-001 — Only the buyer of a delivered order requests a return

(OBJ-11; RG-03)

## BR-RET-002 — A line is never returned beyond what was purchased

Previous non-rejected requests count against it. (*Domain Model*, `ReturnRequest`)

## BR-RET-003 — Refundable amounts use the frozen purchase price

(*Domain Model*, `ReturnItem`)

## BR-RET-004 — Only an approved return originates a refund, exactly one

(*Domain Model*, `Refund`)

## BR-RET-005 — Returned physical units go back to the warehouse they were sold from

(*Domain Model*, `ReturnRequest`)

## BR-RET-006 — Only an administrator decides returns and refunds

(Matriz de Responsabilidades)

## BR-RET-007 — The refund decision records the administrator who took it

(*Domain Model*, `Refund.processedBy`)

---

# Java Implementation

```text
domain/
├── models/
│   ├── ReturnRequest.java                     request, totalRefundableAmount, approve, reject, complete
│   └── Refund.java                            originateFrom, process, reject
├── exceptions/
│   ├── InvalidReturnException.java
│   └── InvalidRefundException.java
├── ports/out/
│   ├── ReturnRequestRepositoryPort.java
│   └── RefundRepositoryPort.java
└── services/returns/
    ├── RequestReturnService.java              ReturnRequest requestReturn(User requestingUser, ReturnRequest request)
    ├── ApproveReturnService.java              Refund approveReturn(User requestingUser, ReturnRequest request)
    ├── RejectReturnService.java               ReturnRequest rejectReturn(User requestingUser, ReturnRequest request)
    ├── CompleteReturnService.java             ReturnRequest completeReturn(User requestingUser, ReturnRequest request)
    ├── ProcessRefundService.java              Refund processRefund(User requestingUser, Refund refund)
    ├── RejectRefundService.java               Refund rejectRefund(User requestingUser, Refund refund)
    └── ConsultReturnsAndRefundsService.java   List<ReturnRequest> consultReturns(User requestingUser)
                                               Refund consultRefund(User requestingUser, ReturnRequest request)
```

`returns` is used as the package name because `return` is a reserved word in Java.

---

# Architectural Constraints

1. `ReturnRequest`, `ReturnItem`, and `Refund` are Domain Models; their rules live in the entities.
2. Services receive Domain Models, never primitive identifiers, amounts, DTOs, or persistence entities.
3. Amounts are computed from the frozen order lines, never received.
4. Stock returns only through Register Inventory Return.
5. Every state change registers an `Operation` and its `AuditLog`.
6. All return and refund rules must remain testable without infrastructure.
