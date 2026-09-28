package aplication.nexusMarket.domain.services.operation;

import aplication.nexusMarket.domain.exceptions.InvalidOperationException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OperationRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Read-only consultation of operations for the Supervisor and the Administrator (OBJ-12). */
@Service
@RequiredArgsConstructor
public class ConsultOperationsService {

    private final OperationRepositoryPort operationRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;

    public List<Operation> executeByPerformer(User requestingUser, User performer) {
        authorize(requestingUser);
        if (performer == null || performer.getUserId() == null) {
            throw new InvalidOperationException("The performing user to consult must be provided.");
        }
        return operationRepositoryPort.findByPerformedBy(performer);
    }

    public List<Operation> executeByAffectedEntity(User requestingUser, Operation criteria) {
        authorize(requestingUser);
        if (criteria == null || criteria.getAffectedEntityType() == null || criteria.getAffectedEntityId() == null) {
            throw new InvalidOperationException("The affected entity type and identifier must be provided.");
        }
        return operationRepositoryPort.findByAffectedEntity(criteria);
    }

    private void authorize(User requestingUser) {
        User authoritativeUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(authoritativeUser, SystemRole.SUPERVISOR, SystemRole.ADMINISTRATOR);
    }
}
