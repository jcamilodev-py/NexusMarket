package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataInventoryMovementRepository extends JpaRepository<InventoryMovementJpaEntity, String> {

    List<InventoryMovementJpaEntity> findByInventoryIdOrderByMovementDateAsc(String inventoryId);
}
