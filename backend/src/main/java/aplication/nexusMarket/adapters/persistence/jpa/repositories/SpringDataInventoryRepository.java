package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.InventoryJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataInventoryRepository extends JpaRepository<InventoryJpaEntity, String> {

    Optional<InventoryJpaEntity> findByVariantIdAndWarehouseId(String variantId, String warehouseId);

    List<InventoryJpaEntity> findByVariantId(String variantId);

    boolean existsByVariantId(String variantId);
}
