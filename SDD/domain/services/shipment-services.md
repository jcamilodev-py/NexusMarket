# Shipment Services

## Introduction

This document defines the services belonging to the **Shipment Management** subdomain of the NexusMarket marketplace platform.

The services in this subdomain are responsible for:

- Creating the shipments that carry the physical lines of a paid order.
- Dispatching a shipment from its warehouse.
- Confirming the delivery of a shipment.
- Consulting the shipments of an order.

**Source:** OBJ-10 ("Gestionar los procesos logísticos"); Sección 4.1 ("Envíos: Procesos logísticos para productos físicos"); Sección 5 ("Operador Logístico: Encargado de la operación física de bodegas y despachos"); Sección 6.1 steps 7–8 ("Se realiza el empaque, despacho y transporte del pedido"; "El pedido se marca como finalizado tras la entrega confirmada"); Matriz de Responsabilidades ("Gestión de Pedidos → Op. Logístico").

The services operate exclusively with **Domain Models** and **Value Objects**, and communicate with anything external to the Domain through **Output Ports**.

---

# Domain Model Context

```text
Shipment
├── identifier
├── order             : Order
├── items             : List<OrderItem>        physical lines only
├── originWarehouse   : Warehouse
├── logisticsOperator : LogisticsOperator
├── shipmentStatus    : ShipmentStatus         PENDING ──> IN_TRANSIT ──> DELIVERED
├── dispatchDate
└── deliveryDate
```

A shipment leaves from exactly one warehouse. An order whose physical lines were reserved from different warehouses therefore produces several shipments, one per warehouse (*Domain Model*, `Shipment`). Each line knows its warehouse through `OrderItem.sourceInventory.warehouse`, which is what makes that grouping possible.

---

# Service Design Principles

### Incorrect

```java
createShipment(String orderId, String warehouseId, List<String> lineIds);
```

### Correct

```java
createShipment(User requestingUser, Shipment shipment);   // shipment carries order and originWarehouse
```

The lines are never received: a shipment carries **every** physical line of the order that was reserved from its origin warehouse and is not yet in another shipment. The operator chooses the warehouse; the Domain decides which lines leave from it, so no line can be forgotten, duplicated, or shipped from a warehouse that does not hold its stock.

---

# Domain Behavior

| Method                                                                                   | Rule it enforces                                                                                         |
| ---------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `List<OrderItem> Order.unshippedPhysicalItemsFrom(Warehouse warehouse)`                  | The physical lines reserved from the warehouse and not yet in a shipment (`order-services.md`).          |
| `static Shipment Shipment.prepare(Order order, Warehouse origin, List<OrderItem> items, LogisticsOperator operator)` | The order is `PAID` or `DISPATCHED`; there is at least one line; starts `PENDING`.   |
| `void Order.addShipment(Shipment shipment)`                                              | Adds the shipment to the order.                                                                          |
| `void Shipment.dispatch()`                                                               | Only from `PENDING`; stamps `dispatchDate`.                                                              |
| `void Shipment.confirmDelivery()`                                                        | Only from `IN_TRANSIT`; stamps `deliveryDate`.                                                           |
| `boolean Shipment.isDelivered()`                                                         | Used by `Order.isReadyForDelivery()`.                                                                    |

A shipment may be created while the order is already `DISPATCHED`: its first shipment has left, and the lines stocked in another warehouse still need theirs.

---

# 1. Create Shipment

## Description

Creates a shipment for a paid order, grouping every physical line reserved from the chosen origin warehouse that is not yet in another shipment. The logistics operator who creates it becomes responsible for it.

Digital lines never belong to a shipment (DOMINIO 5).

**Performed by:** Logistics Operator.

**Source:** Sección 6.1 step 7 ("empaque"); OBJ-10; *Domain Model*, `Shipment`.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (LOGISTICS_OPERATOR)
 │
 ▼
OrderRepositoryPort.findById(shipment.order)
 │
 ▼
WarehouseRepositoryPort.findById(shipment.originWarehouse)
 │
 ▼
items = order.unshippedPhysicalItemsFrom(warehouse)
 │
 ▼
newShipment = Shipment.prepare(order, warehouse, items, operator)      rejects no lines or an unpaid order
 │
 ▼
ShipmentRepositoryPort.save ──> order.addShipment
 │
 ▼
Register Operation and Audit (SHIPMENT_CREATION)
```

Details: `orderId`, `warehouseId`, `itemCount`.

---

# 2. Dispatch Shipment

## Description

Marks a shipment as having left the warehouse, changing its status from `PENDING` to `IN_TRANSIT`.

Dispatching registers the sale outbound of the stock of each line (`inventory-services.md`) and, for the first shipment of an order, moves the order to `DISPATCHED` (`order-services.md`).

**Performed by:** Logistics Operator. Any logistics operator may dispatch or deliver a shipment, not only the one who created it: the specification assigns the physical operation to the role, not to a person, and requiring the same operator would stop a shipment whenever that operator is unavailable.

**Source:** DOMINIO 7 ("Despachado: Salida física de la bodega"); DOMINIO 6 ("Salida por venta"); Sección 6.1 step 7.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (LOGISTICS_OPERATOR)
 │
 ▼
ShipmentRepositoryPort.findById
 │
 ▼
shipment.dispatch()
 │
 ▼
For each line: Register Sale Outbound (operator, line)
 │
 ▼
ShipmentRepositoryPort.update
 │
 ▼
Register Operation and Audit (SHIPMENT_DISPATCH)
 │
 ▼
Mark Order Dispatched (operator, order)        first shipment only
```

Details: `orderId`, `warehouseId`.

---

# 3. Confirm Shipment Delivery

## Description

Confirms that a shipment was delivered, changing its status from `IN_TRANSIT` to `DELIVERED`. When the last shipment of an order is delivered, the order moves to `DELIVERED` and can no longer be modified.

**Performed by:** Logistics Operator.

**Source:** Sección 6.1 step 8 ("El pedido se marca como finalizado tras la entrega confirmada"); DOMINIO 7.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (LOGISTICS_OPERATOR)
 │
 ▼
ShipmentRepositoryPort.findById
 │
 ▼
shipment.confirmDelivery() ──> ShipmentRepositoryPort.update
 │
 ▼
Register Operation and Audit (SHIPMENT_DELIVERY)
 │
 ▼
Mark Order Delivered (operator, order)         only when every physical line is delivered
```

Details: `orderId`.

---

# 4. Consult Shipments

## Description

Retrieves the shipments of an order and their status.

**Performed by:** The buyer who placed the order; Logistics Operator, Administrator, and Supervisor over any order.

---

## Processing

```text
Validate User Status ──> Validate Role Permission (BUYER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR)
 │
 ▼
OrderRepositoryPort.findById ──> buyer ownership when the requester is a buyer
 │
 ▼
ShipmentRepositoryPort.findByOrder
 │
 ▼
List<Shipment>
```

---

# Output Ports

```text
ShipmentRepositoryPort
OrderRepositoryPort       (order-services.md)
WarehouseRepositoryPort   (warehouse-services.md)
```

## ShipmentRepositoryPort

```java
public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(Shipment shipment);

    List<Shipment> findByOrder(Order order);

    void update(Shipment shipment);
}
```

`findById` returns the shipment with its order — including the order's lines and every one of its shipments — because dispatching and delivering decide the order's status from all of them.

---

# Validation Matrix

| Service                   | Requesting user | Role                                                 | Ownership        | State rule                                   |
| ------------------------- | --------------- | ---------------------------------------------------- | ---------------- | -------------------------------------------- |
| Create Shipment           | ACTIVE          | LOGISTICS_OPERATOR                                   | —                | Order PAID or DISPATCHED; unshipped lines in that warehouse |
| Dispatch Shipment         | ACTIVE          | LOGISTICS_OPERATOR                                   | —                | Shipment PENDING                             |
| Confirm Shipment Delivery | ACTIVE          | LOGISTICS_OPERATOR                                   | —                | Shipment IN_TRANSIT                          |
| Consult Shipments         | ACTIVE          | BUYER, LOGISTICS_OPERATOR, ADMINISTRATOR, SUPERVISOR | Buyer: own order | —                                            |

## Service-to-Port Matrix

| Service                   | ShipmentRepositoryPort   | OrderRepositoryPort | WarehouseRepositoryPort | Internal services invoked                    |
| ------------------------- | ------------------------ | ------------------- | ----------------------- | -------------------------------------------- |
| Create Shipment           | `save`                   | `findById`          | `findById`              |                                              |
| Dispatch Shipment         | `findById`, `update`     |                     |                         | Register Sale Outbound, Mark Order Dispatched |
| Confirm Shipment Delivery | `findById`, `update`     |                     |                         | Mark Order Delivered                          |
| Consult Shipments         | `findByOrder`            | `findById`          |                         |                                              |

---

# Input Ports

| Service                                                  | Exposed to                                             |
| -------------------------------------------------------- | ------------------------------------------------------ |
| Create Shipment, Dispatch Shipment, Confirm Shipment Delivery | Logistics Operator                                |
| Consult Shipments                                        | Buyer, Logistics Operator, Administrator, Supervisor   |

---

# Exceptions

| Exception                          | Raised when                                                                                  |
| ---------------------------------- | -------------------------------------------------------------------------------------------- |
| `InvalidShipmentException`         | The order is not paid; the warehouse holds no unshipped line of the order.                   |
| `InvalidStatusTransitionException` | Dispatch or delivery is requested from a status that does not allow it.                      |
| `EntityNotFoundException`          | The order, warehouse, or shipment does not exist.                                            |
| `UnauthorizedOperationException`   | The requesting user is not `ACTIVE`, lacks the required role, or does not own the order.     |

---

# Business Rules Summary

## BR-SHP-001 — Shipments exist only for paid orders

(Sección 6.1 steps 6–7)

## BR-SHP-002 — A shipment leaves from one warehouse and carries every unshipped physical line reserved there

## BR-SHP-003 — Digital lines never ship

(DOMINIO 5)

## BR-SHP-004 — Every physical line belongs to exactly one shipment

(*Domain Model*, `Shipment`)

## BR-SHP-005 — Dispatch removes the reserved stock

(DOMINIO 6)

## BR-SHP-006 — The first dispatch moves the order to DISPATCHED; the last delivery moves it to DELIVERED

(DOMINIO 7; Sección 6.1 step 8)

## BR-SHP-007 — Only logistics operators operate shipments

(Sección 5; Matriz de Responsabilidades)

---

# Java Implementation

```text
domain/
├── models/
│   ├── Shipment.java                         prepare, dispatch, confirmDelivery, isDelivered
│   └── Order.java                            unshippedPhysicalItemsFrom, addShipment
├── exceptions/
│   └── InvalidShipmentException.java
├── ports/out/
│   └── ShipmentRepositoryPort.java
└── services/shipment/
    ├── CreateShipmentService.java            Shipment createShipment(User requestingUser, Shipment shipment)
    ├── DispatchShipmentService.java          Shipment dispatchShipment(User requestingUser, Shipment shipment)
    ├── ConfirmShipmentDeliveryService.java   Shipment confirmShipmentDelivery(User requestingUser, Shipment shipment)
    └── ConsultShipmentsService.java          List<Shipment> consultShipments(User requestingUser, Order order)
```

---

# Architectural Constraints

1. `Shipment` is a Domain Model; its lifecycle rules live in `Shipment`.
2. Services receive Domain Models, never primitive identifiers, DTOs, or persistence entities.
3. The lines of a shipment are decided by the Domain from the order and the warehouse.
4. Stock leaves inventory only through Register Sale Outbound, at dispatch.
5. Every state change registers an `Operation` and its `AuditLog`.
6. All shipment rules must remain testable without infrastructure.
