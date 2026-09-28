package aplication.nexusMarket.domain.services.returns;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
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

/** Rejects a return; its quantities become returnable again in a later request. */
@Service
@RequiredArgsConstructor
public class RejectReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public ReturnRequest rejectReturn(User requestingUser, ReturnRequest request) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (request == null || request.getIdentifier() == null) {
            throw new EntityNotFoundException("Return request");
        }
        ReturnRequest storedRequest = returnRequestRepositoryPort.findById(request)
                .orElseThrow(() -> new EntityNotFoundException("Return request"));

        storedRequest.reject();
        returnRequestRepositoryPort.update(storedRequest);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.RETURN_REJECTION, administrator,
                        AffectedEntityType.RETURN_REQUEST, storedRequest.getIdentifier()),
                Map.of("orderId", storedRequest.getOrder().getIdentifier()));
        return storedRequest;
    }
}
