package aplication.nexusMarket.domain.services.operation;

import aplication.nexusMarket.domain.exceptions.InvalidAuditLogException;
import aplication.nexusMarket.domain.models.AuditLog;
import aplication.nexusMarket.domain.ports.out.AuditLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Appends an immutable audit record to MongoDB (operation-audit-services.md - Register Audit Log). */
@Service
@RequiredArgsConstructor
public class RegisterAuditLogService {

    private final AuditLogRepositoryPort auditLogRepositoryPort;

    public AuditLog execute(AuditLog auditLog) {
        if (auditLog == null
                || auditLog.getOperationType() == null
                || auditLog.getOperationDate() == null
                || auditLog.getPerformedBy() == null
                || auditLog.getUserRole() == null
                || auditLog.getAffectedEntityType() == null
                || auditLog.getAffectedEntityId() == null) {
            throw new InvalidAuditLogException(
                    "Audit log must carry its type, date, performing user, role and affected entity.");
        }
        return auditLogRepositoryPort.save(auditLog);
    }
}
