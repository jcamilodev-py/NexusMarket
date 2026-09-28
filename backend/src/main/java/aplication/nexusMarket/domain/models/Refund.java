package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidRefundException;
import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.RefundStatus;
import aplication.nexusMarket.domain.valueobjects.ReturnStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the monetary reimbursement processed as a result of an approved return.
 *
 * <p>Modeled separately from {@link ReturnRequest} because the specification separates
 * "devoluciones" - the return of the product - from "reembolsos" - the movement of money - and
 * assigns the refund step specifically to the Administrator, while the return is initiated by the
 * Buyer.
 *
 * <p>{@code Refund} is the financial counterpart of {@link Payment}: one records money leaving the
 * marketplace, the other money entering it.
 *
 * <p>Business rules: exists only for a return request in state APPROVED; amount always equals the
 * sum of the refundableAmount of the return lines, so it never exceeds what the buyer actually paid;
 * currency always matches the currency of the original order; only an Administrator may process it.
 *
 * <p>Source: OBJ-11; Matriz de Responsabilidades.
 */
@Getter
@Setter
@NoArgsConstructor
public class Refund {

    private String identifier;

    private ReturnRequest returnRequest;

    /** Sum of the refundableAmount of the return lines. */
    private BigDecimal amount;

    /** Currency of the refund, taken from the original order. Inferred. */
    private Currency currency;

    private RefundStatus refundStatus;

    /** Administrator who processed the refund. */
    private Administrator processedBy;

    private LocalDateTime processDate;

    /** A refund exists only for an approved return, for exactly what its lines paid. */
    public static Refund originateFrom(ReturnRequest request) {
        if (request == null || !ReturnStatus.APPROVED.equals(request.getReturnStatus())) {
            throw new InvalidRefundException("A refund originates only from an approved return.");
        }
        Refund refund = new Refund();
        refund.returnRequest = request;
        refund.amount = request.totalRefundableAmount();
        refund.currency = request.getOrder().getCurrency();
        refund.refundStatus = RefundStatus.PENDING;
        return refund;
    }

    public void process(Administrator administrator) {
        decide(RefundStatus.PROCESSED, administrator);
    }

    public void reject(Administrator administrator) {
        decide(RefundStatus.REJECTED, administrator);
    }

    /** Executing and denying are both administrative decisions, so both record who took them. */
    private void decide(RefundStatus outcome, Administrator administrator) {
        if (!RefundStatus.PENDING.equals(refundStatus)) {
            throw new InvalidStatusTransitionException("The refund has already been decided.");
        }
        this.refundStatus = outcome;
        this.processedBy = administrator;
        this.processDate = LocalDateTime.now();
    }
}
