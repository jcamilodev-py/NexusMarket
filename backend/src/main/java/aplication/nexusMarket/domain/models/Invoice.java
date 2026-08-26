package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.Currency;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the commercial billing information associated with a completed sale.
 *
 * <p>The invoice documents the sale; it does not record the collection of funds, which belongs to
 * {@link Payment}.
 *
 * <p>Business rules: issued only when the order reaches PAID; at most one invoice per order;
 * immutable once issued, so corrections are expressed as new commercial documents; totalAmount and
 * currency always match those of the order.
 *
 * <p>Source: OBJ-09; Seccion 4.1; Seccion 6.1 step 6.
 */
@Getter
@Setter
@NoArgsConstructor
public class Invoice {

    private String identifier;

    /** Order this invoice was generated for. Every invoice traces back to one transaction. */
    private Order order;

    private Buyer buyer;

    /** Date and time the invoice was issued, once the order reached PAID. */
    private LocalDateTime issueDate;

    /** Total billed amount, taken from Order.totalAmount. */
    private BigDecimal totalAmount;

    /** Currency of the invoice, taken from the order. */
    private Currency currency;
}
