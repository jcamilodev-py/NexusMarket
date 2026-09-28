package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataShipmentRepository extends JpaRepository<ShipmentJpaEntity, String> {

    List<ShipmentJpaEntity> findByOrderId(String orderId);
}
