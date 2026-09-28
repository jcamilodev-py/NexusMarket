package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.CartJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataCartRepository;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import aplication.nexusMarket.domain.ports.out.CartRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * The active cart is found through the buyer who owns it, so a caller can only ever reach the cart of
 * the user it passes - the authoritative requesting user.
 */
@Repository
@RequiredArgsConstructor
public class CartJpaAdapter implements CartRepositoryPort {

    private final SpringDataCartRepository cartRepository;
    private final BuyerRepositoryPort buyerRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;

    @Override
    public Cart save(Cart cart) {
        cart.setIdentifier(Identifiers.orNew(cart.getIdentifier()));
        cartRepository.save(CartJpaMapper.toEntity(cart));
        return cart;
    }

    @Override
    public Optional<Cart> findActiveByBuyer(User buyer) {
        if (buyer == null || buyer.getUserId() == null) {
            return Optional.empty();
        }
        Buyer buyerReference = new Buyer();
        buyerReference.setUserId(buyer.getUserId());
        return buyerRepositoryPort.findById(buyerReference)
                .filter(storedBuyer -> storedBuyer.getActiveCart() != null)
                .flatMap(storedBuyer -> cartRepository.findById(storedBuyer.getActiveCart().getIdentifier())
                        .map(entity -> {
                            Cart cart = CartJpaMapper.toDomain(entity, storedBuyer, this::variant);
                            storedBuyer.setActiveCart(cart);
                            return cart;
                        }));
    }

    @Override
    public void update(Cart cart) {
        cartRepository.save(CartJpaMapper.toEntity(cart));
    }

    private ProductVariant variant(String variantId) {
        ProductVariant variantReference = new ProductVariant();
        variantReference.setVariantId(variantId);
        return productRepositoryPort.findVariantById(variantReference).orElse(null);
    }
}
