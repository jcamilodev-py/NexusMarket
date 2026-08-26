package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the type of business entity affected by an {@code Operation}.
 *
 * <p><b>Why this catalog exists.</b> The banking reference points its Operation at a
 * {@code BankingProduct} abstract root shared by all of its products. NexusMarket has no equivalent
 * business concept: significant events affect entities as different as Order, Payment, Inventory,
 * Shipment, ReturnRequest, Refund, Product, Seller and Warehouse, which share no common business
 * meaning. Introducing an artificial technical root would invent a concept the specification does
 * not support. Instead, Operation and AuditLog qualify the affected entity with this catalog plus
 * the entity identifier, keeping the reference typed and domain-controlled.
 *
 * <p>Source: derived from the entities of the Domain Model that generate operations.
 */
@Getter
public enum AffectedEntityType implements DomainCatalog {

    USER("USER", "User",
            "Identity and access events over any participant of the platform."),
    SELLER("SELLER", "Seller",
            "Commercial onboarding and administration of a vendor. Kept separate from USER because "
                    + "the specification treats seller registration as its own business process."),
    WAREHOUSE("WAREHOUSE", "Warehouse",
            "Registration and administration of a storage location."),
    PRODUCT("PRODUCT", "Product",
            "Catalog events over a product and its variants."),
    INVENTORY("INVENTORY", "Inventory",
            "Stock events over an inventory record."),
    CART("CART", "Cart",
            "Events over the provisional selection of a buyer."),
    ORDER("ORDER", "Order",
            "Events over the lifecycle of an order."),
    PAYMENT("PAYMENT", "Payment",
            "Events over a payment attempt."),
    INVOICE("INVOICE", "Invoice",
            "Events over a commercial billing document."),
    SHIPMENT("SHIPMENT", "Shipment",
            "Events over the logistics fulfilment of an order."),
    RETURN_REQUEST("RETURN_REQUEST", "Return Request",
            "Events over a return request and its lines."),
    REFUND("REFUND", "Refund",
            "Events over a monetary reimbursement.");

    private final String code;
    private final String name;
    private final String description;

    AffectedEntityType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
