package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.entities.InventoryJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataInventoryRepository;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Inventory records, returned with the variant's product and seller and with the warehouse, since
 * ownership and the physical nature of the product are decided from them.
 */
@Repository
@RequiredArgsConstructor
public class InventoryJpaAdapter implements InventoryRepositoryPort {

    private final SpringDataInventoryRepository inventoryRepository;
    private final ProductRepositoryPort productRepositoryPort;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public Inventory save(Inventory inventory) {
        inventory.setIdentifier(Identifiers.orNew(inventory.getIdentifier()));
        inventoryRepository.save(InventoryJpaMapper.toEntity(inventory));
        return inventory;
    }

    @Override
    public Optional<Inventory> findById(Inventory inventory) {
        if (inventory == null || inventory.getIdentifier() == null) {
            return Optional.empty();
        }
        return inventoryRepository.findById(inventory.getIdentifier()).map(this::toDomain);
    }

    @Override
    public Optional<Inventory> findByVariantAndWarehouse(Inventory criteria) {
        return inventoryRepository.findByVariantIdAndWarehouseId(
                        criteria.getVariant().getVariantId(), criteria.getWarehouse().getIdentifier())
                .map(this::toDomain);
    }

    @Override
    public List<Inventory> findByVariant(ProductVariant variant) {
        return inventoryRepository.findByVariantId(variant.getVariantId()).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByVariant(ProductVariant variant) {
        return variant != null && variant.getVariantId() != null && inventoryRepository.existsByVariantId(variant.getVariantId());
    }

    @Override
    public List<Inventory> findAll() {
        return inventoryRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void update(Inventory inventory) {
        inventoryRepository.save(InventoryJpaMapper.toEntity(inventory));
    }

    private Inventory toDomain(InventoryJpaEntity entity) {
        ProductVariant variantReference = new ProductVariant();
        variantReference.setVariantId(entity.getVariantId());
        return InventoryJpaMapper.toDomain(entity,
                productRepositoryPort.findVariantById(variantReference).orElse(null),
                referenceResolver.warehouse(entity.getWarehouseId()));
    }
}
