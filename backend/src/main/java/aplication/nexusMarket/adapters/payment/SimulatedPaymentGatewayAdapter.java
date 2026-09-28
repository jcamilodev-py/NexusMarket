package aplication.nexusMarket.adapters.payment;

import aplication.nexusMarket.domain.models.Payment;
import aplication.nexusMarket.domain.ports.out.PaymentGatewayPort;
import aplication.nexusMarket.domain.valueobjects.PaymentStatus;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stands in for the financial party the specification requires but never names (inferred). A real
 * provider is another adapter for the same port; the Domain does not change.
 */
@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Set<PaymentStatus> OUTCOMES =
            Set.of(PaymentStatus.APPROVED, PaymentStatus.REJECTED, PaymentStatus.FAILED);

    private final PaymentStatus simulatedOutcome;

    public SimulatedPaymentGatewayAdapter(@Value("${nexusmarket.payment.simulated-outcome:APPROVED}") String outcome) {
        PaymentStatus status = PaymentStatus.valueOf(outcome);
        if (!OUTCOMES.contains(status)) {
            throw new IllegalArgumentException("The simulated payment outcome must be APPROVED, REJECTED or FAILED.");
        }
        this.simulatedOutcome = status;
    }

    @Override
    public PaymentStatus validate(Payment payment) {
        return simulatedOutcome;
    }
}
