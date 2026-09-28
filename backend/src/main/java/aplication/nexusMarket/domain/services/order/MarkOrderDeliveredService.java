package aplication.nexusMarket.domain.services.order;

import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Internal: closes the order once it is ready for delivery - a paid digital-only order, or every
 * physical line delivered. From then on it can no longer change (Seccion 11).
 */
@Service
@RequiredArgsConstructor
public class MarkOrderDeliveredService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Order execute(User performedBy, Order order) {
        if (!order.isReadyForDelivery()) {
            return order;
        }
        order.markDelivered();
        orderRepositoryPort.update(order);
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.ORDER_DELIVERY, performedBy,
                        AffectedEntityType.ORDER, order.getIdentifier()),
                Map.of());
        return order;
    }
}
