package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A cart line has no identity of its own: it is identified by its variant within the cart. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemEmbeddable {

    @Column(name = "variant_id", nullable = false, length = 36)
    private String variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
