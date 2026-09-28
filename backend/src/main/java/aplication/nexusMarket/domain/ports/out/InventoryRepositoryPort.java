package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.ProductVariant;

public interface InventoryRepositoryPort {

    boolean existsByVariant(ProductVariant variant);
}
