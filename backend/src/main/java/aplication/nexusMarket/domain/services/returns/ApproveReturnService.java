package aplication.nexusMarket.domain.services.returns;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.RefundRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ReturnRequestRepositoryPort;
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
 * Approves a return and originates its pending refund. Executed by an Administrator, inferred: the
 * buyer cannot approve their own request (Matriz de Responsabilidades).
 */
@Service
@RequiredArgsConstructor
public class ApproveReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final RefundRepositoryPort refundRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Refund approveReturn(User requestingUser, ReturnRequest request) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (request == null || request.getIdentifier() == null) {
            throw new EntityNotFoundException("Return request");
        }
        ReturnRequest storedRequest = returnRequestRepositoryPort.findById(request)
                .orElseThrow(() -> new EntityNotFoundException("Return request"));

        storedRequest.approve();
        returnRequestRepositoryPort.update(storedRequest);
        Refund refund = refundRepositoryPort.save(Refund.originateFrom(storedRequest));

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.RETURN_APPROVAL, administrator,
                        AffectedEntityType.RETURN_REQUEST, storedRequest.getIdentifier()),
                Map.of("refundId", refund.getIdentifier(), "refundAmount", refund.getAmount(),
                        "currency", refund.getCurrency().getCode()));
        return refund;
    }
}
