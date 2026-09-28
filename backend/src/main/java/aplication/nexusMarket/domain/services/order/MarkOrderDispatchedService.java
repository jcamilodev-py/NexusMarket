package aplication.nexusMarket.domain.services.order;

import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Internal: the first dispatched shipment moves the order to DISPATCHED; later shipments of the same
 * order change nothing at order level (order-services.md - Mark Order Dispatched).
 */
@Service
@RequiredArgsConstructor
public class MarkOrderDispatchedService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Order execute(User performedBy, Order order) {
        if (!OrderStatus.PAID.equals(order.getOrderStatus())) {
            return order;
        }
        order.markDispatched();
        orderRepositoryPort.update(order);
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.ORDER_DISPATCH, performedBy,
                        AffectedEntityType.ORDER, order.getIdentifier()),
                Map.of());
        return order;
    }
}
