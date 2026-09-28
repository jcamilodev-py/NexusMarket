package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A returned line, identified by the variant of the order line it returns. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ReturnItemEmbeddable {

    @Column(name = "variant_id", nullable = false, length = 36)
    private String variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "refundable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundableAmount;
}
