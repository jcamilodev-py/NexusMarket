package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.RefundStatus;
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
}
