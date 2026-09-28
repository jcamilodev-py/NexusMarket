package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidBuyerException;
import aplication.nexusMarket.domain.exceptions.InvalidCartException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the provisional, editable selection of product variants a buyer makes before checkout.
 *
 * <p>A cart carries no commercial commitment and can be freely modified, unlike an {@link Order}.
 * Modeled as a separate entity because OBJ-07 and OBJ-08 are listed as two distinct functional
 * objectives, which implies two distinct concepts.
 *
 * <p>Business rules: a buyer has exactly one active cart at any given time; a cart may only contain
 * variants of products in state PUBLISHED; confirming a cart creates an order and closes the cart,
 * which is never modified afterwards; a cart reserves no inventory - reservation occurs when the
 * order is created.
 *
 * <p>Source: OBJ-07; DOMINIO 7; Seccion 6.1 step 5.
 */
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    /** Uniquely identifies the cart. Inferred. */
    private String identifier;

    private Buyer buyer;

    private List<CartItem> cartItems = new ArrayList<>();

    /** Date and time the cart was created. Inferred. */
    private LocalDateTime creationDate;

    /** A new cart belongs to exactly one buyer and starts empty. */
    public static Cart openFor(Buyer buyer) {
        if (buyer == null || buyer.getUserId() == null) {
            throw new InvalidBuyerException("A cart can only be opened for a registered buyer.");
        }
        Cart cart = new Cart();
        cart.buyer = buyer;
        cart.creationDate = LocalDateTime.now();
        return cart;
    }

    /**
     * Adds a published variant, merging it into its existing line. A cart holds a single currency
     * because the order it becomes has one currency and one total (inferred).
     */
    public CartItem addItem(ProductVariant variant, int quantity) {
        requirePositive(quantity);
        if (variant == null || variant.getProduct() == null || !variant.getProduct().isPublished()) {
            throw new InvalidCartException("Only variants of published products can be added to a cart.");
        }
        boolean differentCurrency = cartItems.stream()
                .anyMatch(item -> !Objects.equals(item.getVariant().getProduct().getCurrency(), variant.getProduct().getCurrency()));
        if (differentCurrency) {
            throw new InvalidCartException("A cart holds products priced in a single currency.");
        }
        Optional<CartItem> existingLine = findLine(variant);
        if (existingLine.isPresent()) {
            existingLine.get().setQuantity(existingLine.get().getQuantity() + quantity);
            return existingLine.get();
        }
        CartItem line = new CartItem();
        line.setCart(this);
        line.setVariant(variant);
        line.setQuantity(quantity);
        cartItems.add(line);
        return line;
    }

    /** Returns the previous quantity. A line is removed with removeItem, never set to zero. */
    public int updateItemQuantity(ProductVariant variant, int quantity) {
        requirePositive(quantity);
        CartItem line = findLine(variant)
                .orElseThrow(() -> new InvalidCartException("The variant is not in the cart."));
        int previousQuantity = line.getQuantity();
        if (previousQuantity == quantity) {
            throw new InvalidCartException("The line already has this quantity.");
        }
        line.setQuantity(quantity);
        return previousQuantity;
    }

    public CartItem removeItem(ProductVariant variant) {
        CartItem line = findLine(variant)
                .orElseThrow(() -> new InvalidCartException("The variant is not in the cart."));
        cartItems.remove(line);
        return line;
    }

    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    /** Valued at current catalog prices: a cart never freezes a price. */
    public BigDecimal calculateTotal() {
        return cartItems.stream()
                .map(item -> item.getVariant().getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> findLine(ProductVariant variant) {
        if (variant == null || variant.getVariantId() == null) {
            throw new InvalidCartException("The variant must be provided.");
        }
        return cartItems.stream()
                .filter(item -> variant.getVariantId().equals(item.getVariant().getVariantId()))
                .findFirst();
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidCartException("Quantity must be greater than zero.");
        }
    }
}
