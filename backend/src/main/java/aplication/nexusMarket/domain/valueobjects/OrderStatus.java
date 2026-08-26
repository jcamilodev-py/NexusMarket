package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the current stage of an order's lifecycle.
 *
 * <p><b>Note on CART.</b> DOMINIO 7 lists "Carrito" as the first stage of the order lifecycle. In
 * this model that stage is materialized by the {@code Cart} entity, so no {@code Order} instance
 * ever holds this value: an order is created directly in PENDING_PAYMENT. The value is kept so the
 * catalog remains faithful to the five stages the specification enumerates literally.
 *
 * <p>Lifecycle:
 *
 * <pre>
 * CART -&gt; PENDING_PAYMENT -&gt; PAID -&gt; DISPATCHED -&gt; DELIVERED
 *              |               |
 *              +-&gt; CANCELLED   +-&gt; CANCELLED
 *                              |
 *                              +-&gt; DELIVERED  (digital-only orders)
 * </pre>
 *
 * <p>Source: DOMINIO 7; Seccion 11. CANCELLED is inferred: an order whose payment never reaches
 * APPROVED requires a terminal state.
 */
@Getter
public enum OrderStatus implements DomainCatalog {

    CART("CART", "Cart",
            "Provisional selection of products. Stage represented by the Cart entity; "
                    + "no Order instance holds this value."),
    PENDING_PAYMENT("PENDING_PAYMENT", "Pending Payment",
            "Order created, awaiting financial confirmation."),
    PAID("PAID", "Paid",
            "Payment confirmed; the fulfilment process begins and the invoice is issued."),
    DISPATCHED("DISPATCHED", "Dispatched",
            "The first shipment of the order has physically left the warehouse."),
    DELIVERED("DELIVERED", "Delivered / Finished",
            "Every shipment has been delivered; the order is closed and can no longer be modified."),
    CANCELLED("CANCELLED", "Cancelled",
            "Order terminated without completing delivery.");

    private final String code;
    private final String name;
    private final String description;

    OrderStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
