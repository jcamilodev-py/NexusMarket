package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.BillingJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataInvoiceRepository;
import aplication.nexusMarket.domain.models.Invoice;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.ports.out.InvoiceRepositoryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Invoices are immutable once issued, so there is no update. */
@Repository
@RequiredArgsConstructor
public class InvoiceJpaAdapter implements InvoiceRepositoryPort {

    private final SpringDataInvoiceRepository invoiceRepository;

    @Override
    public Invoice save(Invoice invoice) {
        invoice.setIdentifier(Identifiers.orNew(invoice.getIdentifier()));
        invoiceRepository.save(BillingJpaMapper.toEntity(invoice));
        return invoice;
    }

    @Override
    public Optional<Invoice> findByOrder(Order order) {
        return invoiceRepository.findByOrderId(order.getIdentifier())
                .map(entity -> BillingJpaMapper.toDomain(entity, order));
    }
}
