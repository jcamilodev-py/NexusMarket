package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Cart;

public interface CartRepositoryPort {

    Cart save(Cart cart);
}
