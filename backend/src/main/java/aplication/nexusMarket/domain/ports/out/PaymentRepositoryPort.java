package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Payment;
import java.util.List;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);

    List<Payment> findByOrder(Order order);

    void update(Payment payment);
}
