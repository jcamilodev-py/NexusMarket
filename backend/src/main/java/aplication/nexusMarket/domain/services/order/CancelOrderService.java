package aplication.nexusMarket.domain.services.order;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.OrderItem;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.inventory.ReleaseInventoryReservationService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Cancels an unpaid order and releases its reservations. Once paid, the only reimbursement path the
 * specification describes is a return (OBJ-11), so Order.cancel rejects anything but PENDING_PAYMENT.
 */
@Service
@RequiredArgsConstructor
public class CancelOrderService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;
    private final ReleaseInventoryReservationService releaseInventoryReservationService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Order cancelOrder(User requestingUser, Order order) {
        User buyer = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(buyer, SystemRole.BUYER);
        if (order == null || order.getIdentifier() == null) {
            throw new EntityNotFoundException("Order");
        }
        Order storedOrder = orderRepositoryPort.findById(order)
                .orElseThrow(() -> new EntityNotFoundException("Order"));
        validateBuyerOwnershipService.execute(buyer, storedOrder.getBuyer());

        storedOrder.cancel();
        List<OrderItem> reservedLines = storedOrder.getOrderItems().stream()
                .filter(item -> item.getSourceInventory() != null)
                .toList();
        reservedLines.forEach(item -> releaseInventoryReservationService.execute(buyer, item));
        orderRepositoryPort.update(storedOrder);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.ORDER_CANCELLATION, buyer,
                        AffectedEntityType.ORDER, storedOrder.getIdentifier()),
                Map.of("releasedLineCount", reservedLines.size()));
        return storedOrder;
    }
}
