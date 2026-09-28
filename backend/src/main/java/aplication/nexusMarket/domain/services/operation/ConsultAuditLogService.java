package aplication.nexusMarket.domain.services.operation;

import aplication.nexusMarket.domain.exceptions.InvalidAuditLogException;
import aplication.nexusMarket.domain.models.AuditLog;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.AuditLogRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Read-only consultation of the audit trail for the Supervisor and the Administrator (OBJ-12). */
@Service
@RequiredArgsConstructor
public class ConsultAuditLogService {

    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;

    public List<AuditLog> executeByPerformer(User requestingUser, User performer) {
        authorize(requestingUser);
        if (performer == null || performer.getUserId() == null) {
            throw new InvalidAuditLogException("The performing user to consult must be provided.");
        }
        return auditLogRepositoryPort.findByPerformedBy(performer);
    }

    public List<AuditLog> executeByAffectedEntity(User requestingUser, AuditLog criteria) {
        authorize(requestingUser);
        if (criteria == null || criteria.getAffectedEntityType() == null || criteria.getAffectedEntityId() == null) {
            throw new InvalidAuditLogException("The affected entity type and identifier must be provided.");
        }
        return auditLogRepositoryPort.findByAffectedEntity(criteria);
    }

    public List<AuditLog> executeByOperationType(User requestingUser, AuditLog criteria) {
        authorize(requestingUser);
        if (criteria == null || criteria.getOperationType() == null) {
            throw new InvalidAuditLogException("The operation type to consult must be provided.");
        }
        return auditLogRepositoryPort.findByOperationType(criteria);
    }

    private void authorize(User requestingUser) {
        User authoritativeUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(authoritativeUser, SystemRole.SUPERVISOR, SystemRole.ADMINISTRATOR);
    }
}
