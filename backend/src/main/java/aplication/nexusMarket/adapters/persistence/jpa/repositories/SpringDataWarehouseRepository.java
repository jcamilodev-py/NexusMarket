package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataWarehouseRepository extends JpaRepository<WarehouseJpaEntity, String> {

    List<WarehouseJpaEntity> findByOwnerId(String ownerId);
}
