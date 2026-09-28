# Payment and Billing Services

## Introduction

This document defines the services belonging to the **Payment and Billing Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Registering payment attempts for an order and obtaining their financial validation.
- Issuing the invoice of a paid order.
- Consulting payments and invoices.

`Payment` records money entering the marketplace; `Invoice` is the commercial document that supports the sale. Neither replaces the other (*Domain Model*, **Money In and Money Out Are Both Recorded**).

**Source:** OBJ-09 ("Administrar la facturación de las compras"); Sección 4.1 ("Facturación: Información comercial asociada a las ventas"); DOMINIO 7 ("Pendiente de Pago: Espera de confirmación financiera"; "Pagado: Inicio de procesos de alistamiento"); Sección 6.1 step 6 ("Se valida el pago y se inicia el flujo de preparación").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

```text
Order
├── payments : List<Payment>        one per attempt
│                 ├── amount, currency       equal to the order's
│                 ├── paymentStatus          PENDING ──> APPROVED | REJECTED | FAILED
│                 └── paymentDate
└── invoice  : Invoice              at most one, issued when the order reaches PAID
                  ├── buyer : Buyer
                  ├── issueDate
                  └── totalAmount, currency  equal to the order's
```

Each payment attempt is its own record, so a rejected attempt is never overwritten by a retry (*Domain Model*, `Payment`). At most one attempt per order reaches `APPROVED`: once it does, the order is `PAID` and accepts no further attempts.

---

# Financial Validation

The specification requires the payment to be validated ("Se valida el pago", "Espera de confirmación financiera") but assigns that validation to no participant of Sección 5, and none of them has financial responsibilities.

**Inferred:** the validation is performed by an external financial party reached through an Output Port, `PaymentGatewayPort`. The Domain decides what each outcome means for the order; the port only reports the outcome. The means of payment is not modeled, since the specification does not describe it (*Domain Model*, `Payment`).

---

# Service Design Principles

### Incorrect

```java
pay(String orderId, BigDecimal amount);
```

### Correct

```java
registerPayment(User requestingUser, Payment payment);   // payment carries its order
```

The amount and currency are never received: they are taken from the order, since partial payments are not contemplated (*Domain Model*, `Payment`).

---

# Domain Behavior

| Method                                                | Rule it enforces                                                                                           |
| ----------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `static Payment Payment.attemptFor(Order order)`      | The order is `PENDING_PAYMENT`; amount and currency are the order's; starts `PENDING`, stamped with its date. |
| `void Payment.resolve(PaymentStatus outcome)`         | Only from `PENDING`, and only to `APPROVED`, `REJECTED`, or `FAILED`.                                     |
| `boolean Payment.isApproved()`                        | Whether the attempt settled the order.                                                                    |
| `void Order.registerPaymentAttempt(Payment payment)`  | Adds the attempt to the order's history.                                                                  |
| `static Invoice Invoice.issueFor(Order order)`        | The order is `PAID`; buyer, amount, and currency are the order's; stamped with its issue date.            |
| `void Order.attachInvoice(Invoice invoice)`           | An order has at most one invoice.                                                                         |

---

# 1. Register Payment

## Description

Registers a payment attempt for an order in state `PENDING_PAYMENT`, for the full amount of the order, and submits it for financial validation.

The outcome resolves the attempt as `APPROVED`, `REJECTED`, or `FAILED`:

* `APPROVED` confirms the payment of the order (**Confirm Order Payment**, `order-services.md`), which issues the invoice.
* `REJECTED` and `FAILED` leave the order unchanged; the attempt remains as a historical record and the buyer may register a new one.

**Performed by:** Buyer, over their own orders.

**Source:** Sección 6.1 step 6; DOMINIO 7; *Domain Model*, `Payment`.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER)
 │
 ▼
OrderRepositoryPort.findById(payment.order) ──> Validate Buyer Ownership
 │
 ▼
attempt = Payment.attemptFor(order)            rejects an order that is not PENDING_PAYMENT
 │
 ▼
PaymentRepositoryPort.save ──> order.registerPaymentAttempt(attempt)
 │
 ▼
Register Operation and Audit (PAYMENT_REGISTRATION)
 │
 ▼
outcome = PaymentGatewayPort.validate(attempt)
 │
 ▼
attempt.resolve(outcome) ──> PaymentRepositoryPort.update
 │
 ▼
Register Operation and Audit (PAYMENT_APPROVAL | PAYMENT_REJECTION | PAYMENT_FAILURE)
 │
 ▼
attempt.isApproved()? ──yes──> Confirm Order Payment (buyer, order)
```

The attempt is persisted **before** it is validated, so that an attempt interrupted by an infrastructure failure still leaves a trace.

---

## Operation and Audit

| Operation              | When                         | `affectedEntityType` | `details`                       |
| ---------------------- | ---------------------------- | -------------------- | ------------------------------- |
| `PAYMENT_REGISTRATION` | Always                       | `PAYMENT`            | `orderId`, `amount`, `currency` |
| `PAYMENT_APPROVAL`     | Outcome `APPROVED`           | `PAYMENT`            | `orderId`                       |
| `PAYMENT_REJECTION`    | Outcome `REJECTED`           | `PAYMENT`            | `orderId`                       |
| `PAYMENT_FAILURE`      | Outcome `FAILED`             | `PAYMENT`            | `orderId`                       |

All of them are performed by the buyer.

---

# 2. Consult Payments

## Description

Retrieves the payment attempts registered for an order.

**Performed by:** The buyer who placed the order; Administrator and Supervisor over any order.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER, ADMINISTRATOR, SUPERVISOR)
 │
 ▼
OrderRepositoryPort.findById ──> buyer ownership when the requester is a buyer
 │
 ▼
PaymentRepositoryPort.findByOrder
 │
 ▼
List<Payment>
```

---

# 3. Issue Invoice

## Description

Issues the invoice of an order when it reaches `PAID`, taking its buyer, total amount, and currency from the order. An order has at most one invoice, and an issued invoice is immutable.

**Performed by:** Internal — triggered by **Confirm Order Payment**, on behalf of the buyer.

**Source:** OBJ-09; Sección 4.1; Sección 6.1 step 6; *Domain Model*, `Invoice`.

---

## Processing

```text
invoice = Invoice.issueFor(order)             rejects an order that is not PAID
 │
 ▼
order.attachInvoice(invoice)                  rejects a second invoice
 │
 ▼
InvoiceRepositoryPort.save
 │
 ▼
Register Operation and Audit (INVOICE_ISSUANCE)
```

Details: `orderId`, `totalAmount`, `currency`.

---

# 4. Consult Invoice

## Description

Retrieves the invoice of an order.

**Performed by:** The buyer who was billed; Administrator and Supervisor over any invoice.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER, ADMINISTRATOR, SUPERVISOR)
 │
 ▼
OrderRepositoryPort.findById ──> buyer ownership when the requester is a buyer
 │
 ▼
InvoiceRepositoryPort.findByOrder ──> none? ──> EntityNotFoundException
 │
 ▼
Invoice
```

---

# Output Ports

```text
PaymentRepositoryPort
PaymentGatewayPort
InvoiceRepositoryPort
OrderRepositoryPort     (order-services.md)
```

## PaymentRepositoryPort

```java
public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    List<Payment> findByOrder(Order order);

    void update(Payment payment);
}
```

`update` only ever persists the resolution of a `PENDING` attempt; a resolved attempt is never changed again.

## PaymentGatewayPort

```java
public interface PaymentGatewayPort {

    PaymentStatus validate(Payment payment);
}
```

Returns `APPROVED`, `REJECTED`, or `FAILED`. The adapter maps the external party's answer to these values; an unreachable party is reported as `FAILED`, never as an exception that would lose the attempt.

## InvoiceRepositoryPort

```java
public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrder(Order order);
}
```

Invoices are immutable: the port offers no update nor delete.

---

# Validation Matrix

| Service          | Requesting user | Role                             | Ownership                  | State rule                                  |
| ---------------- | --------------- | -------------------------------- | -------------------------- | ------------------------------------------- |
| Register Payment | ACTIVE          | BUYER                            | Own order                  | Order PENDING_PAYMENT                       |
| Consult Payments | ACTIVE          | BUYER, ADMINISTRATOR, SUPERVISOR | Buyer: own order           | —                                           |
| Issue Invoice    | Internal        | —                                | —                          | Order PAID; no previous invoice             |
| Consult Invoice  | ACTIVE          | BUYER, ADMINISTRATOR, SUPERVISOR | Buyer: own order           | Invoice exists                              |

## Service-to-Port Matrix

| Service          | PaymentRepositoryPort        | PaymentGatewayPort | InvoiceRepositoryPort | OrderRepositoryPort | Internal services invoked |
| ---------------- | ---------------------------- | ------------------ | --------------------- | ------------------- | ------------------------- |
| Register Payment | `save`, `update`             | `validate`         |                       | `findById`          | Confirm Order Payment     |
| Consult Payments | `findByOrder`                |                    |                       | `findById`          |                           |
| Issue Invoice    |                              |                    | `save`                |                     |                           |
| Consult Invoice  |                              |                    | `findByOrder`         | `findById`          |                           |

---

# Input Ports

| Service                              | Exposed to                         |
| ------------------------------------ | ---------------------------------- |
| Register Payment                     | Buyer                              |
| Consult Payments, Consult Invoice    | Buyer, Administrator, Supervisor   |
| Issue Invoice                        | Internal — not exposed             |

---

# Exceptions

| Exception                          | Raised when                                                                              |
| ---------------------------------- | ---------------------------------------------------------------------------------------- |
| `InvalidPaymentException`          | The order is not `PENDING_PAYMENT`; the outcome is not a final status; the attempt is not `PENDING`. |
| `InvalidInvoiceException`          | The order is not `PAID`, or it already has an invoice.                                   |
| `EntityNotFoundException`          | The order or the invoice does not exist.                                                 |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, lacks the required role, or does not own the order. |

---

# Business Rules Summary

## BR-PAY-001 — Every attempt is its own record

A rejected or failed attempt is never overwritten. (*Domain Model*, `Payment`)

## BR-PAY-002 — The amount is always the order's total

Partial payments are not contemplated. (*Domain Model*, `Payment`)

## BR-PAY-003 — Only an order in PENDING_PAYMENT accepts attempts

So at most one attempt per order reaches `APPROVED`.

## BR-PAY-004 — Only an approved attempt moves the order to PAID

(Sección 6.1 step 6)

## BR-PAY-005 — Financial validation is external

Inferred; see **Financial Validation**.

## BR-PAY-006 — An invoice is issued only for a PAID order, at most once, and never changes

(*Domain Model*, `Invoice`)

---

# Java Implementation

```text
domain/
├── models/
│   ├── Payment.java                        attemptFor, resolve, isApproved
│   ├── Invoice.java                        issueFor
│   └── Order.java                          registerPaymentAttempt, attachInvoice
├── exceptions/
│   ├── InvalidPaymentException.java
│   └── InvalidInvoiceException.java
├── ports/out/
│   ├── PaymentRepositoryPort.java
│   ├── PaymentGatewayPort.java
│   └── InvoiceRepositoryPort.java
└── services/billing/
    ├── RegisterPaymentService.java         Payment registerPayment(User requestingUser, Payment payment)
    ├── ConsultPaymentsService.java         List<Payment> consultPayments(User requestingUser, Order order)
    ├── IssueInvoiceService.java            Invoice execute(User performedBy, Order order)
    └── ConsultInvoiceService.java          Invoice consultInvoice(User requestingUser, Order order)
```

---

# Architectural Constraints

1. `Payment` and `Invoice` are Domain Models with their own lifecycles; neither replaces the other.
2. Services receive Domain Models, never primitive identifiers, amounts, DTOs, or persistence entities.
3. Amounts and currencies are always taken from the order.
4. The financial validation is obtained only through `PaymentGatewayPort`.
5. Payments and invoices are never edited after they are final.
6. Every state change registers an `Operation` and its `AuditLog`.
7. All payment and billing rules must remain testable without infrastructure.
