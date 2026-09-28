package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Payment;
import aplication.nexusMarket.domain.valueobjects.PaymentStatus;

/**
 * The external financial party (inferred: no participant of Seccion 5 validates payments). Returns
 * APPROVED, REJECTED or FAILED; an unreachable party is FAILED, never an exception that loses the attempt.
 */
public interface PaymentGatewayPort {

    PaymentStatus validate(Payment payment);
}
