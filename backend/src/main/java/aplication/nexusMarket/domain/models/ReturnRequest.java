package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidReturnException;
import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
import aplication.nexusMarket.domain.valueobjects.ReturnStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the request of a buyer to return one or more purchased items from an order.
 *
 * <p>The request states exactly which order lines are being returned and in what quantity, through
 * its {@link ReturnItem} lines. Without that detail the amount to be refunded could not be
 * determined, since a buyer may return only part of an order.
 *
 * <p>Business rules: may only be created by the buyer who owns the order, and only over an order
 * that has been delivered; the returned quantity of a line may never exceed the quantity purchased,
 * discounting quantities already returned; an approved request over physical items generates an
 * InventoryMovement of type RETURN over the sourceInventory of each returned line, so units go back
 * to the warehouse they were sold from; it originates a {@link Refund} only when approved.
 *
 * <p>Source: OBJ-11; Matriz de Responsabilidades; DOMINIO 6.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequest {

    private String identifier;

    private Order order;

    /** Buyer who requested the return; must be the buyer of the order. */
    private Buyer requestedBy;

    /** Lines being returned. At least one element. Inferred from "uno o mas productos". */
    private List<ReturnItem> returnItems = new ArrayList<>();

    private String reason;

    private LocalDateTime requestDate;

    private ReturnStatus returnStatus;

    /**
     * Builds a request over a delivered order. Each line is matched to the order line of its variant
     * and may not exceed what was purchased minus what earlier non-rejected requests already claim;
     * amounts use the frozen unit price, so the refund repays what was actually paid.
     */
    public static ReturnRequest request(Order order, List<ReturnItem> items, String reason,
                                        List<ReturnRequest> previousRequests) {
        if (order == null || !OrderStatus.DELIVERED.equals(order.getOrderStatus())) {
            throw new InvalidReturnException("Only a delivered order can be returned.");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidReturnException("A return request needs at least one line.");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidReturnException("A return request needs a reason.");
        }
        ReturnRequest request = new ReturnRequest();
        Set<String> requestedVariants = new HashSet<>();
        for (ReturnItem item : items) {
            OrderItem orderLine = findOrderLine(order, item);
            String variantId = orderLine.getVariant().getVariantId();
            if (!requestedVariants.add(variantId)) {
                throw new InvalidReturnException("Each order line appears once per return request.");
            }
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            int stillReturnable = orderLine.getQuantity() - alreadyRequested(previousRequests, variantId);
            if (quantity <= 0 || quantity > stillReturnable) {
                throw new InvalidReturnException("Returned quantity must be between 1 and " + stillReturnable + ".");
            }
            ReturnItem line = new ReturnItem();
            line.setReturnRequest(request);
            line.setOrderItem(orderLine);
            line.setQuantity(quantity);
            line.setRefundableAmount(orderLine.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            request.returnItems.add(line);
        }
        request.order = order;
        request.requestedBy = order.getBuyer();
        request.reason = reason;
        request.requestDate = LocalDateTime.now();
        request.returnStatus = ReturnStatus.REQUESTED;
        return request;
    }

    public BigDecimal totalRefundableAmount() {
        return returnItems.stream().map(ReturnItem::getRefundableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void approve() {
        requireStatus("approve", ReturnStatus.REQUESTED);
        this.returnStatus = ReturnStatus.APPROVED;
    }

    public void reject() {
        requireStatus("reject", ReturnStatus.REQUESTED);
        this.returnStatus = ReturnStatus.REJECTED;
    }

    public void complete() {
        requireStatus("complete", ReturnStatus.APPROVED);
        this.returnStatus = ReturnStatus.COMPLETED;
    }

    private static OrderItem findOrderLine(Order order, ReturnItem item) {
        String variantId = item == null || item.getOrderItem() == null || item.getOrderItem().getVariant() == null
                ? null : item.getOrderItem().getVariant().getVariantId();
        return order.getOrderItems().stream()
                .filter(line -> line.getVariant().getVariantId().equals(variantId))
                .findFirst()
                .orElseThrow(() -> new InvalidReturnException("Every returned line must belong to the order."));
    }

    private static int alreadyRequested(List<ReturnRequest> previousRequests, String variantId) {
        return previousRequests == null ? 0 : previousRequests.stream()
                .filter(previous -> !ReturnStatus.REJECTED.equals(previous.getReturnStatus()))
                .flatMap(previous -> previous.getReturnItems().stream())
                .filter(line -> line.getOrderItem().getVariant().getVariantId().equals(variantId))
                .mapToInt(ReturnItem::getQuantity)
                .sum();
    }

    private void requireStatus(String action, ReturnStatus expected) {
        if (!expected.equals(returnStatus)) {
            throw new InvalidStatusTransitionException("Cannot " + action + " a return request in status "
                    + (returnStatus == null ? "none" : returnStatus.getCode()) + ".");
        }
    }
}
