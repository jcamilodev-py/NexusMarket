package aplication.nexusMarket.domain.services.operation;

import aplication.nexusMarket.domain.models.AuditLog;
import aplication.nexusMarket.domain.models.Operation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Single traceability registration point invoked by every service that changes state, always after
 * the change succeeds.
 *
 * <p>The Operation (MySQL) is stored before the AuditLog (MongoDB); a failure in either propagates so
 * the use-case transaction rolls back the business change (operation-audit-services.md - Transactional
 * Consistency). Details must never contain passwords, hashes or tokens.
 */
@Service
@RequiredArgsConstructor
public class RegisterOperationAndAuditService {

    private final RegisterOperationService registerOperationService;
    private final RegisterAuditLogService registerAuditLogService;

    public Operation execute(Operation operation, Map<String, Object> details) {
        Operation persistedOperation = registerOperationService.execute(operation);
        registerAuditLogService.execute(AuditLog.fromOperation(persistedOperation, details));
        return persistedOperation;
    }
}
