package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single product variant line within a cart, before checkout.
 *
 * <p>Unlike {@link OrderItem}, a cart item does not freeze the price: the cart is a provisional
 * selection and always reflects the current catalog price.
 *
 * <p>Business rules: quantity must be greater than zero; the same variant appears at most once per
 * cart, so adding it again increases the quantity of the existing line.
 *
 * <p>Source: OBJ-07; Seccion 6.1 step 5. Line structure inferred, mirroring OrderItem.
 */
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    private Cart cart;

    private ProductVariant variant;

    /** Quantity currently selected. Must be greater than zero. */
    private Integer quantity;
}
