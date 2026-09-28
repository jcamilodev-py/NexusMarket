package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.User;
import java.util.Optional;

/**
 * findActiveByBuyer takes the authoritative requesting user, so a buyer can only reach their own cart;
 * it loads the lines with their variants and products.
 */
public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findActiveByBuyer(User buyer);

    void update(Cart cart);
}
