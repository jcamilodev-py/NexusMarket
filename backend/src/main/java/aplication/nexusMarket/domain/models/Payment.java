package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidPaymentException;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
import aplication.nexusMarket.domain.valueobjects.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an attempt to collect the amount of an order from the buyer.
 *
 * <p>Each attempt is its own record, so a rejected attempt remains as a historical trace rather than
 * being overwritten by a later retry. {@code Payment} is the financial event that settles the sale;
 * {@link Invoice} is the commercial document that supports it, and neither replaces the other.
 *
 * <p>Business rules: amount always equals Order.totalAmount, since partial payments are not
 * contemplated; at most one payment per order may reach APPROVED; an order moves to PAID only when
 * one of its payments reaches APPROVED; a rejected or failed payment never modifies the order.
 *
 * <p>No payment method is modeled: the specification validates that the payment occurred but does
 * not describe the means used.
 *
 * <p>Source: DOMINIO 7; Seccion 6.1 step 6.
 */
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    private String identifier;

    private Order order;

    /** Amount collected, equal to Order.totalAmount. */
    private BigDecimal amount;

    /** Currency in which the payment is expressed. Inferred. */
    private Currency currency;

    private PaymentStatus paymentStatus;

    /** Date and time the payment attempt was registered. Inferred. */
    private LocalDateTime paymentDate;

    private static final Set<PaymentStatus> OUTCOMES =
            Set.of(PaymentStatus.APPROVED, PaymentStatus.REJECTED, PaymentStatus.FAILED);

    /** Always for the order's full amount: partial payments are not contemplated. */
    public static Payment attemptFor(Order order) {
        if (order == null || !OrderStatus.PENDING_PAYMENT.equals(order.getOrderStatus())) {
            throw new InvalidPaymentException("Only an order pending payment accepts payment attempts.");
        }
        Payment payment = new Payment();
        payment.order = order;
        payment.amount = order.getTotalAmount();
        payment.currency = order.getCurrency();
        payment.paymentStatus = PaymentStatus.PENDING;
        payment.paymentDate = LocalDateTime.now();
        return payment;
    }

    /** A resolved attempt is final; a retry is a new Payment, so failed attempts stay on record. */
    public void resolve(PaymentStatus outcome) {
        if (!PaymentStatus.PENDING.equals(paymentStatus)) {
            throw new InvalidPaymentException("The payment attempt is already resolved.");
        }
        if (!OUTCOMES.contains(outcome)) {
            throw new InvalidPaymentException("A payment attempt resolves only as approved, rejected or failed.");
        }
        this.paymentStatus = outcome;
    }

    public boolean isApproved() {
        return PaymentStatus.APPROVED.equals(paymentStatus);
    }
}
