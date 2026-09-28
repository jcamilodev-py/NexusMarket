# Output Ports

## Introduction

This document consolidates the **Output Ports** of the NexusMarket Domain.

An Output Port is an interface owned by the Domain that states what the Domain needs from the outside world — storing and reading business entities, hashing passwords, issuing authentication tokens, validating payments — without stating how it is done. Each port is implemented by an Output Adapter outside the Domain.

Every port listed here was introduced by the service document that first needed it (`SDD/domain/services/`). This document gathers their final contracts in one place, so that the persistence and integration adapters can be implemented against a single reference.

All ports live in:

```text
domain/ports/out/
```

---

# Architectural Rule

The dependency flow must always be:

```text
Domain Service
      │
      ▼
Output Port
      │
      ▼
Output Adapter
      │
      ▼
External Resource
```

For example:

```text
PlaceOrderService
      │
      ▼
OrderRepositoryPort
      │
      ▼
Order MySQL adapter (JPA)
      │
      ▼
MySQL
```

For auditing:

```text
RegisterAuditLogService
      │
      ▼
AuditLogRepositoryPort
      │
      ▼
AuditLog MongoDB adapter
      │
      ▼
MongoDB
```

The Domain never imports a framework, a driver, or a library type through a port: every port exposes only Domain Models, Value Objects, and standard Java types.

---

# General Parameter Rule

Output Port methods work with Domain Models and Value Objects. They must not receive DTOs, persistence entities, or primitive identifiers when the corresponding Domain Model already exists.

Incorrect:

```java
Optional<Order> findById(String orderId);
```

Correct:

```java
Optional<Order> findById(Order order);
```

Incorrect:

```java
List<Order> findByPeriod(LocalDate from, LocalDate to);
```

Correct:

```java
List<Order> findByPeriod(ReportPeriod period);
```

Where a query needs a criterion that is not a whole entity, the criterion travels inside a Domain Model used as an example (`findByStatus(Order criteria)`) or inside a dedicated Value Object (`ReportPeriod`, `Credentials`).

---

# Storage Map

| Store                                | Ports                                                                                                   |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------- |
| MySQL — `nexusmarket_db`             | User, Buyer, Seller, Warehouse, Product, Inventory, InventoryMovement, Cart, Order, Payment, Invoice, Shipment, ReturnRequest, Refund, Operation |
| MongoDB — `nexusmarket_audit.audit_logs` | AuditLog                                                                                            |
| Security infrastructure              | PasswordService, JwtService                                                                            |
| External financial party             | PaymentGateway                                                                                          |

Only `AuditLog` lives in MongoDB, as in the banking reference: it is an append-only historical record with flexible, operation-specific details.

---

# Common Conventions

* `save` assigns the identifier of a new entity and returns it.
* `update` persists the current state of an existing entity. It is never offered for records that are immutable once created: `InventoryMovement`, `Invoice`, and `AuditLog` ports have no `update` and no port has a `delete`.
* `findById` returns `Optional.empty()` when nothing is found; the calling service decides whether that is an error.
* Lookups return the entity with the relationships its consumers need, as stated for each port. Relationships not listed are loaded on demand, as the *Domain Model* states for `Buyer.orders` and `Seller.products`.

---

# Output Ports

## 1. UserRepositoryPort

### Responsibility

Stores and reads every user of the platform, regardless of role. Lookups return the concrete specialization that matches the stored role.

### Methods

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

### Main Consumers

Validate User Status (every service), Login, Register Staff User, Register Buyer, Register Seller, Consult User, Change User Status.

---

## 2. BuyerRepositoryPort

### Responsibility

Stores and reads buyers with their buyer-specific attributes: addresses, commercial status, and active cart.

### Methods

```java
public interface BuyerRepositoryPort {

    Buyer save(Buyer buyer);

    Optional<Buyer> findById(Buyer buyer);

    void update(Buyer buyer);
}
```

### Main Consumers

Register Buyer, Consult Buyer Profile, Update Buyer Addresses, Change Buyer Commercial Status, Place Order.

---

## 3. SellerRepositoryPort

### Responsibility

Stores and reads sellers with their commercial identity. `findById` loads the seller's warehouses but not their products.

### Methods

```java
public interface SellerRepositoryPort {

    Seller save(Seller seller);

    Optional<Seller> findById(Seller seller);

    void update(Seller seller);
}
```

### Main Consumers

Register Seller, Consult Seller, Update Seller Information, Register Seller Warehouse.

---

## 4. WarehouseRepositoryPort

### Responsibility

Stores and reads warehouses of both kinds. `save` returns the same specialization it receives; `findById` returns `MarketplaceWarehouse` or `SellerWarehouse` with its owner.

### Methods

```java
public interface WarehouseRepositoryPort {

    <T extends Warehouse> T save(T warehouse);

    Optional<Warehouse> findById(Warehouse warehouse);

    void update(Warehouse warehouse);
}
```

### Main Consumers

Register Seller, Register Marketplace Warehouse, Register Seller Warehouse, Consult Warehouse, Update Warehouse, Register Inventory Inbound, Create Shipment.

---

## 5. ProductRepositoryPort

### Responsibility

Stores and reads products with their variants. `save` assigns product and variant identifiers and returns the same specialization; `findById` returns the product with its seller and variants; `findVariantById` returns a variant with its product and the product's seller.

### Methods

```java
public interface ProductRepositoryPort {

    <T extends Product> T save(T product);

    Optional<Product> findById(Product product);

    List<Product> findPublished();

    Optional<ProductVariant> findVariantById(ProductVariant variant);

    boolean existsBySku(ProductVariant variant);

    void update(Product product);
}
```

### Main Consumers

Catalog services, Register Inventory Inbound, Consult Inventory, Add Item to Cart.

---

## 6. InventoryRepositoryPort

### Responsibility

Stores and reads inventory records. Every lookup returns the record with its variant, the variant's product and seller, and its warehouse.

### Methods

```java
public interface InventoryRepositoryPort {

    Inventory save(Inventory inventory);

    Optional<Inventory> findById(Inventory inventory);

    Optional<Inventory> findByVariantAndWarehouse(Inventory criteria);

    List<Inventory> findByVariant(ProductVariant variant);

    boolean existsByVariant(ProductVariant variant);

    List<Inventory> findAll();

    void update(Inventory inventory);
}
```

### Main Consumers

Inventory services, Publish Product, Consult Inventory Report.

---

## 7. InventoryMovementRepositoryPort

### Responsibility

Appends and reads inventory movements. Movements are immutable: no update, no delete.

### Methods

```java
public interface InventoryMovementRepositoryPort {

    InventoryMovement save(InventoryMovement movement);

    List<InventoryMovement> findByInventory(Inventory inventory);
}
```

### Main Consumers

Every inventory service that changes quantities; Consult Inventory Movements.

---

## 8. CartRepositoryPort

### Responsibility

Stores and reads carts. `findActiveByBuyer` receives the authoritative requesting user, so a buyer can only reach their own active cart, and loads its lines with their variants and products.

### Methods

```java
public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findActiveByBuyer(User buyer);

    void update(Cart cart);
}
```

### Main Consumers

Register Buyer, Cart services, Place Order.

---

## 9. OrderRepositoryPort

### Responsibility

Stores and reads orders. Lookups return the order with its buyer, its lines (variant, product, seller, and source inventory), payments, invoice, and shipments. `findByStatus` returns every order when the criterion carries no status.

### Methods

```java
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(Order order);

    List<Order> findByBuyer(User buyer);

    List<Order> findBySeller(User seller);

    List<Order> findByStatus(Order criteria);

    List<Order> findByPeriod(ReportPeriod period);

    void update(Order order);
}
```

### Main Consumers

Order services, Register Payment, Consult Payments, Consult Invoice, Create Shipment, Consult Shipments, Request Return, Consult Sales Report.

---

## 10. PaymentRepositoryPort

### Responsibility

Stores and reads payment attempts. `update` only ever persists the resolution of a `PENDING` attempt.

### Methods

```java
public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    List<Payment> findByOrder(Order order);

    void update(Payment payment);
}
```

### Main Consumers

Register Payment, Consult Payments.

---

## 11. PaymentGatewayPort

### Responsibility

Obtains the financial validation of a payment attempt from the external financial party (inferred; see `payment-billing-services.md`). Returns `APPROVED`, `REJECTED`, or `FAILED`; an unreachable party is reported as `FAILED`, never as an exception that would lose the attempt.

### Methods

```java
public interface PaymentGatewayPort {

    PaymentStatus validate(Payment payment);
}
```

### Main Consumers

Register Payment.

---

## 12. InvoiceRepositoryPort

### Responsibility

Stores and reads invoices. Invoices are immutable once issued: no update, no delete.

### Methods

```java
public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrder(Order order);
}
```

### Main Consumers

Issue Invoice, Consult Invoice.

---

## 13. ShipmentRepositoryPort

### Responsibility

Stores and reads shipments. `findById` returns the shipment with its order, including the order's lines and every one of its shipments, because the order's status is decided from all of them.

### Methods

```java
public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(Shipment shipment);

    List<Shipment> findByOrder(Order order);

    void update(Shipment shipment);
}
```

### Main Consumers

Shipment services.

---

## 14. ReturnRequestRepositoryPort

### Responsibility

Stores and reads return requests. Lookups return the request with its order, buyer, and lines — each line with its order line, variant, product, and source inventory.

### Methods

```java
public interface ReturnRequestRepositoryPort {

    ReturnRequest save(ReturnRequest request);

    Optional<ReturnRequest> findById(ReturnRequest request);

    List<ReturnRequest> findByOrder(Order order);

    List<ReturnRequest> findByBuyer(User buyer);

    List<ReturnRequest> findAll();

    List<ReturnRequest> findByPeriod(ReportPeriod period);

    void update(ReturnRequest request);
}
```

### Main Consumers

Return and refund services, Consult Returns and Refunds Report.

---

## 15. RefundRepositoryPort

### Responsibility

Stores and reads refunds.

### Methods

```java
public interface RefundRepositoryPort {

    Refund save(Refund refund);

    Optional<Refund> findById(Refund refund);

    Optional<Refund> findByReturnRequest(ReturnRequest request);

    void update(Refund refund);
}
```

### Main Consumers

Approve Return, Process Refund, Reject Refund, Consult Returns and Refunds, Consult Returns and Refunds Report.

---

## 16. OperationRepositoryPort

### Responsibility

Appends and reads business operations in MySQL.

### Methods

```java
public interface OperationRepositoryPort {

    Operation save(Operation operation);

    List<Operation> findByPerformedBy(User user);

    List<Operation> findByAffectedEntity(Operation criteria);
}
```

### Main Consumers

Register Operation, Consult Operations.

---

## 17. AuditLogRepositoryPort

### Responsibility

Appends and reads audit records in MongoDB. Audit records are append-only: no update, no delete. The adapter stores the performer and the affected entity as denormalized values, so the trail remains readable even if the referenced records change.

### Methods

```java
public interface AuditLogRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByPerformedBy(User user);

    List<AuditLog> findByAffectedEntity(AuditLog criteria);

    List<AuditLog> findByOperationType(AuditLog criteria);
}
```

### Main Consumers

Register Audit Log, Consult Audit Log.

---

## 18. PasswordServicePort

### Responsibility

Hashes passwords and verifies them against stored hashes. The implementation may use BCrypt, Argon2, or PBKDF2; none of them enters the Domain.

### Methods

```java
public interface PasswordServicePort {

    String encode(Credentials credentials);

    boolean matches(Credentials credentials, User user);
}
```

### Main Consumers

Login, Register Staff User, Register Buyer, Register Seller.

---

## 19. JwtServicePort

### Responsibility

Issues the authentication token of a user, carrying `userId`, `email`, `fullName`, and `role`. Unlike the banking reference, the port exposes no JWT library type: validating a token and rebuilding the requesting user belong to the input adapter.

### Methods

```java
public interface JwtServicePort {

    String generateToken(User user);
}
```

### Main Consumers

Login.

---

# Port Summary

| #  | Port                            | Store / resource         | Methods |
| -- | ------------------------------- | ------------------------ | ------- |
| 1  | UserRepositoryPort              | MySQL                    | 6       |
| 2  | BuyerRepositoryPort             | MySQL                    | 3       |
| 3  | SellerRepositoryPort            | MySQL                    | 3       |
| 4  | WarehouseRepositoryPort         | MySQL                    | 3       |
| 5  | ProductRepositoryPort           | MySQL                    | 6       |
| 6  | InventoryRepositoryPort         | MySQL                    | 7       |
| 7  | InventoryMovementRepositoryPort | MySQL                    | 2       |
| 8  | CartRepositoryPort              | MySQL                    | 3       |
| 9  | OrderRepositoryPort             | MySQL                    | 7       |
| 10 | PaymentRepositoryPort           | MySQL                    | 3       |
| 11 | PaymentGatewayPort              | External financial party | 1       |
| 12 | InvoiceRepositoryPort           | MySQL                    | 2       |
| 13 | ShipmentRepositoryPort          | MySQL                    | 4       |
| 14 | ReturnRequestRepositoryPort     | MySQL                    | 7       |
| 15 | RefundRepositoryPort            | MySQL                    | 4       |
| 16 | OperationRepositoryPort         | MySQL                    | 3       |
| 17 | AuditLogRepositoryPort          | MongoDB                  | 4       |
| 18 | PasswordServicePort             | Security infrastructure  | 2       |
| 19 | JwtServicePort                  | Security infrastructure  | 1       |

---

# Architectural Constraints

1. Output Ports are interfaces owned by the Domain and live in `domain/ports/out/`.
2. Output Adapters implement them outside the Domain.
3. Ports receive and return Domain Models, Value Objects, and standard Java types only.
4. No port exposes a framework, driver, or library type.
5. No port deletes; immutable records have no update.
6. Only `AuditLog` is stored in MongoDB; every other entity is stored in MySQL.
7. Domain services depend on ports, never on adapters.
