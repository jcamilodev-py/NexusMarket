package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.ProductVariant;
import java.util.List;
import java.util.Optional;

/** Lookups return the record with its variant, the variant's product and seller, and its warehouse. */
public interface InventoryRepositoryPort {

    Inventory save(Inventory inventory);

    Optional<Inventory> findById(Inventory inventory);

    Optional<Inventory> findByVariantAndWarehouse(Inventory criteria);

    List<Inventory> findByVariant(ProductVariant variant);

    boolean existsByVariant(ProductVariant variant);

    void update(Inventory inventory);
}
