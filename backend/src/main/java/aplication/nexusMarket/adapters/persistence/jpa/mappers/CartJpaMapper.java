package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.jpa.entities.CartItemEmbeddable;
import aplication.nexusMarket.adapters.persistence.jpa.entities.CartJpaEntity;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.CartItem;
import aplication.nexusMarket.domain.models.ProductVariant;
import java.util.ArrayList;
import java.util.function.Function;

public final class CartJpaMapper {

    private CartJpaMapper() {
    }

    public static CartJpaEntity toEntity(Cart domain) {
        CartJpaEntity entity = new CartJpaEntity();
        entity.setCartId(domain.getIdentifier());
        entity.setBuyerId(domain.getBuyer().getUserId());
        entity.setCreationDate(domain.getCreationDate());
        entity.setItems(new ArrayList<>(domain.getCartItems().stream()
                .map(item -> new CartItemEmbeddable(item.getVariant().getVariantId(), item.getQuantity()))
                .toList()));
        return entity;
    }

    public static Cart toDomain(CartJpaEntity entity, Buyer buyer, Function<String, ProductVariant> variants) {
        Cart domain = new Cart();
        domain.setIdentifier(entity.getCartId());
        domain.setBuyer(buyer);
        domain.setCreationDate(entity.getCreationDate());
        for (CartItemEmbeddable line : entity.getItems()) {
            CartItem item = new CartItem();
            item.setCart(domain);
            item.setVariant(variants.apply(line.getVariantId()));
            item.setQuantity(line.getQuantity());
            domain.getCartItems().add(item);
        }
        return domain;
    }
}
