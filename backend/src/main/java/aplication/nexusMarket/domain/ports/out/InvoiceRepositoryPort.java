package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Invoice;
import aplication.nexusMarket.domain.models.Order;
import java.util.Optional;

/** Offers no update nor delete: an issued invoice is immutable. */
public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrder(Order order);
}
