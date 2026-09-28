package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.BillingJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataPaymentRepository;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Payment;
import aplication.nexusMarket.domain.ports.out.PaymentRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Payment attempts, returned in the order they were made. */
@Repository
@RequiredArgsConstructor
public class PaymentJpaAdapter implements PaymentRepositoryPort {

    private final SpringDataPaymentRepository paymentRepository;

    @Override
    public Payment save(Payment payment) {
        payment.setIdentifier(Identifiers.orNew(payment.getIdentifier()));
        paymentRepository.save(BillingJpaMapper.toEntity(payment));
        return payment;
    }

    @Override
    public List<Payment> findByOrder(Order order) {
        return paymentRepository.findByOrderIdOrderByPaymentDateAsc(order.getIdentifier()).stream()
                .map(entity -> BillingJpaMapper.toDomain(entity, order))
                .toList();
    }

    @Override
    public void update(Payment payment) {
        paymentRepository.save(BillingJpaMapper.toEntity(payment));
    }
}
