package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
}
