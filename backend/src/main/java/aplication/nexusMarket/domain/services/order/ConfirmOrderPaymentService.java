package aplication.nexusMarket.domain.services.order;

import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.billing.IssueInvoiceService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Internal: an approved payment moves the order to PAID and issues its invoice; a digital-only order
 * is then delivered at once, since digital products are delivered right after payment (DOMINIO 5).
 */
@Service
@RequiredArgsConstructor
public class ConfirmOrderPaymentService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final IssueInvoiceService issueInvoiceService;
    private final MarkOrderDeliveredService markOrderDeliveredService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Order execute(User performedBy, Order order) {
        order.confirmPayment();
        orderRepositoryPort.update(order);
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.ORDER_PAYMENT_CONFIRMATION, performedBy,
                        AffectedEntityType.ORDER, order.getIdentifier()),
                Map.of());

        issueInvoiceService.execute(performedBy, order);
        return markOrderDeliveredService.execute(performedBy, order);
    }
}
