package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.PaymentJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, String> {

    List<PaymentJpaEntity> findByOrderIdOrderByPaymentDateAsc(String orderId);
}
