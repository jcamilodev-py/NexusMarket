package aplication.nexusMarket.domain.services.returns;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.Administrator;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.RefundRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Executes a pending refund, recording the administrator who did it (Matriz de Responsabilidades).
 * It does not wait for the goods: the specification sets no order between receipt and refund.
 */
@Service
@RequiredArgsConstructor
public class ProcessRefundService {

    private final RefundRepositoryPort refundRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Refund processRefund(User requestingUser, Refund refund) {
        User requester = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(requester, SystemRole.ADMINISTRATOR);
        if (!(requester instanceof Administrator administrator)) {
            throw new UnauthorizedOperationException("Only an administrator processes refunds.");
        }
        if (refund == null || refund.getIdentifier() == null) {
            throw new EntityNotFoundException("Refund");
        }
        Refund storedRefund = refundRepositoryPort.findById(refund)
                .orElseThrow(() -> new EntityNotFoundException("Refund"));

        storedRefund.process(administrator);
        refundRepositoryPort.update(storedRefund);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.REFUND_PROCESSING, administrator,
                        AffectedEntityType.REFUND, storedRefund.getIdentifier()),
                Map.of("amount", storedRefund.getAmount(), "currency", storedRefund.getCurrency().getCode(),
                        "returnRequestId", storedRefund.getReturnRequest().getIdentifier()));
        return storedRefund;
    }
}
