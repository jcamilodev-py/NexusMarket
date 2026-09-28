package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.RefundJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRefundRepository extends JpaRepository<RefundJpaEntity, String> {

    Optional<RefundJpaEntity> findByReturnRequestId(String returnRequestId);
}
