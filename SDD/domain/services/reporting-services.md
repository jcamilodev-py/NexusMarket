# Reporting Services

## Introduction

This document defines the services belonging to the **Administrative Reporting** subdomain of the NexusMarket marketplace platform.

The specification includes the consultation of administrative reports as a process (Sección 3.1) and consolidating administrative information as an objective (OBJ-12), but it does not enumerate the reports. The three reports below are **inferred**: each one consolidates one of the business flows the specification describes, and together they cover its commercial, stock, and after-sales dimensions.

| Report                          | Flow it consolidates                  | Specification                      |
| ------------------------------- | ------------------------------------- | ---------------------------------- |
| Sales Report                    | Orders and their amounts              | DOMINIO 7; OBJ-08; OBJ-09          |
| Inventory Report                | Distributed stock                     | DOMINIO 6; OBJ-06                  |
| Returns and Refunds Report      | Returns and the money given back      | OBJ-11                             |

All reports are read-only: they change nothing and generate no `Operation`. The audit trail itself is consulted through `operation-audit-services.md`.

**Source:** OBJ-12 ("Consolidar información administrativa para consulta"); Sección 3.1 ("Consulta de reportes administrativos"); Sección 5 ("Supervisor: Perfil de consulta y seguimiento operativo").

---

# Domain Model Context

Reports read existing entities — `Order`, `Inventory`, `ReturnRequest`, `Refund` — and consolidate them into Value Objects defined in *Domain Value Objects*:

```text
ReportPeriod     from, to              the inclusive date range of a report
SalesSummary     status, currency, orderCount, totalAmount
ReturnSummary    status, currency, requestCount, refundedAmount
```

The consolidation is computed by these Value Objects from the entities, so the aggregation rules live in the Domain and never in a query.

Amounts are always grouped by currency as well as by status: adding amounts denominated in different currencies would produce a meaningless figure (*Domain Model*, **Monetary Amounts Are Always Denominated**).

---

# Service Design Principles

### Incorrect

```java
salesReport(String from, String to);
```

### Correct

```java
consultSalesReport(User requestingUser, ReportPeriod period);
```

The period is a Value Object that guarantees its own consistency — both dates present, `from` not after `to` — before any report is computed.

---

# Domain Behavior

| Method                                                                        | Rule it enforces                                                                                     |
| ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `ReportPeriod(LocalDate from, LocalDate to)`                                  | Both dates present; `from` is not after `to`.                                                        |
| `boolean ReportPeriod.contains(LocalDateTime moment)`                         | The moment falls on any day from `from` to `to`, both inclusive.                                     |
| `static List<SalesSummary> SalesSummary.summarize(List<Order> orders)`        | One summary per status and currency: number of orders and sum of their `totalAmount`.                |
| `static List<ReturnSummary> ReturnSummary.summarize(List<ReturnRequest> requests, List<Refund> refunds)` | One summary per return status and currency: number of requests and sum of the refunds actually `PROCESSED`. |

Only `PROCESSED` refunds count as refunded money: a pending refund has not left the marketplace, and a rejected one never will.

---

# 1. Consult Sales Report

## Description

Consolidates the orders created within a period, by status and currency, with the number of orders and their total amount.

**Performed by:** Supervisor and Administrator.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SUPERVISOR, ADMINISTRATOR)
 │
 ▼
period present? ──no──> InvalidReportException
 │
 ▼
OrderRepositoryPort.findByPeriod(period)        orders whose creationDate falls in the period
 │
 ▼
SalesSummary.summarize(orders)
 │
 ▼
List<SalesSummary>
```

---

# 2. Consult Inventory Report

## Description

Consolidates the current stock of the marketplace: every inventory record, with its variant, warehouse, available and reserved quantities, and status. Damaged stock is identified by the status of its record.

The inventory report reflects the present, not a period: stock is a current state, and its history is available through **Consult Inventory Movements** (`inventory-services.md`).

**Performed by:** Supervisor and Administrator.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SUPERVISOR, ADMINISTRATOR)
 │
 ▼
InventoryRepositoryPort.findAll
 │
 ▼
List<Inventory>
```

---

# 3. Consult Returns and Refunds Report

## Description

Consolidates the return requests created within a period, by status and currency, with the number of requests and the amount actually refunded.

**Performed by:** Supervisor and Administrator.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (SUPERVISOR, ADMINISTRATOR)
 │
 ▼
ReturnRequestRepositoryPort.findByPeriod(period)     requests whose requestDate falls in the period
 │
 ▼
For each request: RefundRepositoryPort.findByReturnRequest
 │
 ▼
ReturnSummary.summarize(requests, refunds)
 │
 ▼
List<ReturnSummary>
```

---

# Output Ports — additions

The reporting services add the following queries to ports defined in other documents:

```java
// OrderRepositoryPort (order-services.md)
List<Order> findByPeriod(ReportPeriod period);

// InventoryRepositoryPort (inventory-services.md)
List<Inventory> findAll();

// ReturnRequestRepositoryPort (return-refund-services.md)
List<ReturnRequest> findByPeriod(ReportPeriod period);
```

They also use `RefundRepositoryPort.findByReturnRequest` (`return-refund-services.md`).

---

# Validation Matrix

| Service                            | Requesting user | Role                      | Input                    |
| ---------------------------------- | --------------- | ------------------------- | ------------------------ |
| Consult Sales Report               | ACTIVE          | SUPERVISOR, ADMINISTRATOR | Valid `ReportPeriod`     |
| Consult Inventory Report           | ACTIVE          | SUPERVISOR, ADMINISTRATOR | —                        |
| Consult Returns and Refunds Report | ACTIVE          | SUPERVISOR, ADMINISTRATOR | Valid `ReportPeriod`     |

## Service-to-Port Matrix

| Service                            | OrderRepositoryPort | InventoryRepositoryPort | ReturnRequestRepositoryPort | RefundRepositoryPort     |
| ---------------------------------- | ------------------- | ----------------------- | --------------------------- | ------------------------ |
| Consult Sales Report               | `findByPeriod`      |                         |                             |                          |
| Consult Inventory Report           |                     | `findAll`               |                             |                          |
| Consult Returns and Refunds Report |                     |                         | `findByPeriod`              | `findByReturnRequest`    |

---

# Input Ports

All three services are exposed to the Supervisor and the Administrator.

---

# Exceptions

| Exception                        | Raised when                                                              |
| -------------------------------- | ------------------------------------------------------------------------ |
| `InvalidReportException`         | The period is missing, a date is missing, or `from` is after `to`.       |
| `UnauthorizedOperationException` | The requesting user is not `ACTIVE` or is neither Supervisor nor Administrator. |

---

# Business Rules Summary

## BR-REP-001 — Reports are read-only and generate no operations

## BR-REP-002 — Only the Supervisor and the Administrator consult reports

(Sección 5; OBJ-12)

## BR-REP-003 — Amounts are consolidated per currency, never mixed

(*Domain Model*, **Monetary Amounts Are Always Denominated**)

## BR-REP-004 — Only processed refunds count as refunded money

## BR-REP-005 — Aggregation rules live in the Domain Value Objects, not in queries

---

# Java Implementation

```text
domain/
├── valueobjects/
│   ├── ReportPeriod.java                              record
│   ├── SalesSummary.java                              record, static summarize
│   └── ReturnSummary.java                             record, static summarize
├── exceptions/
│   └── InvalidReportException.java
├── ports/out/                                         findByPeriod / findAll additions
└── services/reporting/
    ├── ConsultSalesReportService.java                 List<SalesSummary> consultSalesReport(User requestingUser, ReportPeriod period)
    ├── ConsultInventoryReportService.java             List<Inventory> consultInventoryReport(User requestingUser)
    └── ConsultReturnsAndRefundsReportService.java     List<ReturnSummary> consultReturnsAndRefundsReport(User requestingUser, ReportPeriod period)
```

---

# Architectural Constraints

1. Reports read Domain Models and return Domain Models or Value Objects.
2. Aggregation is computed by Domain Value Objects, not by persistence queries.
3. Reports change nothing and register no operation.
4. All reporting rules must remain testable without infrastructure.
