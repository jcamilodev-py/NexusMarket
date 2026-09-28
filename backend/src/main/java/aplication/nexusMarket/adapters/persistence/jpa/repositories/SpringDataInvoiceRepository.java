package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, String> {

    Optional<InvoiceJpaEntity> findByOrderId(String orderId);
}
