package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An order line, identified by its variant within the order. Prices are stored as frozen at checkout;
 * sourceInventoryId is empty for digital lines.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class OrderItemEmbeddable {

    @Column(name = "variant_id", nullable = false, length = 36)
    private String variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "source_inventory_id", length = 36)
    private String sourceInventoryId;
}
