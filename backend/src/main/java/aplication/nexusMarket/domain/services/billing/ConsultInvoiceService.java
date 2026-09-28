package aplication.nexusMarket.domain.services.billing;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Invoice;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InvoiceRepositoryPort;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** The invoice of an order, for the billed buyer, administrators and supervisors. */
@Service
@RequiredArgsConstructor
public class ConsultInvoiceService {

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public Invoice consultInvoice(User requestingUser, Order order) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser,
                SystemRole.BUYER, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (order == null || order.getIdentifier() == null) {
            throw new EntityNotFoundException("Order");
        }
        Order storedOrder = orderRepositoryPort.findById(order)
                .orElseThrow(() -> new EntityNotFoundException("Order"));
        if (consultingUser.hasRole(SystemRole.BUYER)) {
            validateBuyerOwnershipService.execute(consultingUser, storedOrder.getBuyer());
        }
        return invoiceRepositoryPort.findByOrder(storedOrder)
                .orElseThrow(() -> new EntityNotFoundException("Invoice"));
    }
}
