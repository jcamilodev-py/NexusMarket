package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the type of significant business operation executed within the marketplace.
 *
 * <p>Operations are independent from entity statuses: a <b>status</b> represents the current state
 * of an entity, while an <b>operation</b> represents an action or event that occurred. For example
 * {@code Order.orderStatus = PAID} describes where the order is now, while
 * {@code Operation.operationType = ORDER_PAYMENT_CONFIRMATION} describes what brought it there.
 *
 * <p>Every significant business action must reference an OperationType, and every operation must be
 * recorded in the AuditLog.
 *
 * <p>Source: derived from the business events identified across the specification, so the AuditLog
 * covers the traceability scope required by OBJ-12 and Seccion 1.
 */
@Getter
public enum OperationType implements DomainCatalog {

    // --- User operations ---
    USER_REGISTRATION("USER_REGISTRATION", "User Registration",
            "A user was registered in the platform."),
    USER_STATUS_CHANGE("USER_STATUS_CHANGE", "User Status Change",
            "The operational status of a user was modified."),
    BUYER_REGISTRATION("BUYER_REGISTRATION", "Buyer Registration",
            "A buyer was registered in the platform."),
    SELLER_REGISTRATION("SELLER_REGISTRATION", "Seller Registration",
            "A seller was onboarded by an administrator, with their first warehouse."),

    // --- Warehouse operations ---
    WAREHOUSE_REGISTRATION("WAREHOUSE_REGISTRATION", "Warehouse Registration",
            "A warehouse was registered."),

    // --- Catalog operations ---
    PRODUCT_REGISTRATION("PRODUCT_REGISTRATION", "Product Registration",
            "A product was registered by its seller."),
    PRODUCT_PUBLICATION("PRODUCT_PUBLICATION", "Product Publication",
            "A product was published to the public catalog."),
    PRODUCT_SUSPENSION("PRODUCT_SUSPENSION", "Product Suspension",
            "A product was temporarily hidden from the catalog."),
    PRODUCT_DISCONTINUATION("PRODUCT_DISCONTINUATION", "Product Discontinuation",
            "A product was permanently removed from active sale."),

    // --- Inventory operations ---
    INVENTORY_INBOUND("INVENTORY_INBOUND", "Inventory Inbound",
            "Stock entered a warehouse."),
    INVENTORY_RESERVATION("INVENTORY_RESERVATION", "Inventory Reservation",
            "Stock was reserved for an order."),
    INVENTORY_SALE_OUTBOUND("INVENTORY_SALE_OUTBOUND", "Inventory Sale Outbound",
            "Stock left a warehouse due to a confirmed sale."),
    INVENTORY_ADJUSTMENT("INVENTORY_ADJUSTMENT", "Inventory Adjustment",
            "Stock quantity was manually corrected."),
    INVENTORY_RETURN("INVENTORY_RETURN", "Inventory Return",
            "Stock re-entered a warehouse due to an approved return."),

    // --- Cart operations ---
    CART_ITEM_ADDITION("CART_ITEM_ADDITION", "Cart Item Addition",
            "A product variant was added to a cart."),
    CART_ITEM_REMOVAL("CART_ITEM_REMOVAL", "Cart Item Removal",
            "A product variant was removed from a cart."),
    CART_CONFIRMATION("CART_CONFIRMATION", "Cart Confirmation",
            "A cart was confirmed, producing an order."),

    // --- Order operations ---
    ORDER_PLACEMENT("ORDER_PLACEMENT", "Order Placement",
            "An order was created from a confirmed cart."),
    ORDER_PAYMENT_CONFIRMATION("ORDER_PAYMENT_CONFIRMATION", "Order Payment Confirmation",
            "Payment for an order was confirmed and the order became paid."),
    ORDER_DISPATCH("ORDER_DISPATCH", "Order Dispatch",
            "The order was dispatched, following its first shipment."),
    ORDER_DELIVERY("ORDER_DELIVERY", "Order Delivery",
            "Every shipment of the order was delivered; the order is closed."),
    ORDER_CANCELLATION("ORDER_CANCELLATION", "Order Cancellation",
            "The order was terminated without completing delivery."),

    // --- Payment operations ---
    PAYMENT_REGISTRATION("PAYMENT_REGISTRATION", "Payment Registration",
            "A payment attempt was registered against an order."),
    PAYMENT_APPROVAL("PAYMENT_APPROVAL", "Payment Approval",
            "A payment attempt was validated successfully."),
    PAYMENT_REJECTION("PAYMENT_REJECTION", "Payment Rejection",
            "A payment attempt was refused or failed."),

    // --- Billing operations ---
    INVOICE_ISSUANCE("INVOICE_ISSUANCE", "Invoice Issuance",
            "An invoice was issued for a paid order."),

    // --- Shipment operations ---
    SHIPMENT_CREATION("SHIPMENT_CREATION", "Shipment Creation",
            "A shipment was created for the physical lines of an order."),
    SHIPMENT_DISPATCH("SHIPMENT_DISPATCH", "Shipment Dispatch",
            "A shipment left a warehouse."),
    SHIPMENT_DELIVERY("SHIPMENT_DELIVERY", "Shipment Delivery",
            "A shipment was confirmed as delivered."),

    // --- Return and refund operations ---
    RETURN_REQUEST_CREATION("RETURN_REQUEST_CREATION", "Return Request Creation",
            "A buyer requested the return of one or more purchased items."),
    RETURN_APPROVAL("RETURN_APPROVAL", "Return Approval",
            "A return request was accepted."),
    RETURN_REJECTION("RETURN_REJECTION", "Return Rejection",
            "A return request was denied."),
    RETURN_COMPLETION("RETURN_COMPLETION", "Return Completion",
            "A returned product was received and the request was closed."),
    REFUND_PROCESSING("REFUND_PROCESSING", "Refund Processing",
            "A refund was executed by an administrator."),
    REFUND_REJECTION("REFUND_REJECTION", "Refund Rejection",
            "A refund was denied during administrative review.");

    private final String code;
    private final String name;
    private final String description;

    OperationType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
