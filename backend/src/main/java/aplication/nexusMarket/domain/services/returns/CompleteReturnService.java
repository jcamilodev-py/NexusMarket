package aplication.nexusMarket.domain.services.returns;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.ReturnItem;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.ReturnRequestRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.inventory.RegisterInventoryReturnService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Receives the returned products (a logistics operator, inferred from Seccion 5) and puts physical
 * units back where they were sold from; digital lines have no stock to return.
 */
@Service
@RequiredArgsConstructor
public class CompleteReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterInventoryReturnService registerInventoryReturnService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public ReturnRequest completeReturn(User requestingUser, ReturnRequest request) {
        User operator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(operator, SystemRole.LOGISTICS_OPERATOR);
        if (request == null || request.getIdentifier() == null) {
            throw new EntityNotFoundException("Return request");
        }
        ReturnRequest storedRequest = returnRequestRepositoryPort.findById(request)
                .orElseThrow(() -> new EntityNotFoundException("Return request"));

        storedRequest.complete();
        List<ReturnItem> physicalLines = storedRequest.getReturnItems().stream()
                .filter(line -> line.getOrderItem().getVariant().getProduct().requiresInventory())
                .toList();
        physicalLines.forEach(line -> registerInventoryReturnService.execute(operator, line));
        returnRequestRepositoryPort.update(storedRequest);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.RETURN_COMPLETION, operator,
                        AffectedEntityType.RETURN_REQUEST, storedRequest.getIdentifier()),
                Map.of("returnedPhysicalLineCount", physicalLines.size()));
        return storedRequest;
    }
}
