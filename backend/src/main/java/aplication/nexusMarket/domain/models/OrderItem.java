package aplication.nexusMarket.domain.models;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single confirmed product variant line within an order, with the quantity and price
 * locked in at the moment of purchase.
 *
 * <p>Business rules: quantity must be greater than zero; unitPrice and subtotal are frozen at
 * checkout and never recalculated, so historical orders remain faithful to the conditions accepted
 * by the buyer; an order item is never modified after the order is created.
 *
 * <p>Source: DOMINIO 7; Seccion 6.1 step 5.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    private Order order;

    /** Variant purchased. The product is reachable through variant.getProduct(). */
    private ProductVariant variant;

    private Integer quantity;

    /** Price at the moment the order was confirmed. Independent from Product.price. */
    private BigDecimal unitPrice;

    /** quantity * unitPrice. */
    private BigDecimal subtotal;
}
