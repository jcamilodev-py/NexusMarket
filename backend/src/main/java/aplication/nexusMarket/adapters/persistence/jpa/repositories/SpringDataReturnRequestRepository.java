package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.ReturnRequestJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataReturnRequestRepository extends JpaRepository<ReturnRequestJpaEntity, String> {

    List<ReturnRequestJpaEntity> findByOrderId(String orderId);

    List<ReturnRequestJpaEntity> findByRequestedById(String requestedById);

    List<ReturnRequestJpaEntity> findByRequestDateGreaterThanEqualAndRequestDateLessThan(LocalDateTime from, LocalDateTime to);
}
