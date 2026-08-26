package aplication.nexusMarket.domain.models;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single line within a return request: which purchased order line is being returned,
 * and how many units of it.
 *
 * <p>This is the structure that makes the refund amount computable, since it links each returned
 * unit back to the price frozen in the original order line.
 *
 * <p>Business rules: quantity must be greater than zero and must not exceed the quantity of the
 * referenced order item, discounting units already returned; refundableAmount is always computed
 * from OrderItem.unitPrice, the price frozen at checkout, never from the current product price; the
 * referenced order item must belong to the order of the return request.
 *
 * <p>Source: OBJ-11. Line structure inferred, mirroring OrderItem.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReturnItem {

    private ReturnRequest returnRequest;

    /** Original order line being returned. The variant and purchase price are reachable through it. */
    private OrderItem orderItem;

    private Integer quantity;

    /** quantity * orderItem.getUnitPrice(). Inferred. */
    private BigDecimal refundableAmount;
}
