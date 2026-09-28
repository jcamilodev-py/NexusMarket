package aplication.nexusMarket.domain.services.billing;

import aplication.nexusMarket.domain.models.Invoice;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InvoiceRepositoryPort;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Internal: issues the single, immutable invoice of a paid order (OBJ-09; Seccion 6.1 step 6). */
@Service
@RequiredArgsConstructor
public class IssueInvoiceService {

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Invoice execute(User performedBy, Order order) {
        Invoice invoice = Invoice.issueFor(order);
        order.attachInvoice(invoice);
        Invoice issuedInvoice = invoiceRepositoryPort.save(invoice);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.INVOICE_ISSUANCE, performedBy,
                        AffectedEntityType.INVOICE, issuedInvoice.getIdentifier()),
                Map.of("orderId", order.getIdentifier(), "totalAmount", order.getTotalAmount(),
                        "currency", order.getCurrency().getCode()));
        return issuedInvoice;
    }
}
