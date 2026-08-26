package aplication.nexusMarket.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
}
