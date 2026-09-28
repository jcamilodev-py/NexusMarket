package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.AuditLog;
import aplication.nexusMarket.domain.models.User;
import java.util.List;

/** Offers no update nor delete: audit records are append-only by contract. */
public interface AuditLogRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByPerformedBy(User user);

    List<AuditLog> findByAffectedEntity(AuditLog criteria);

    List<AuditLog> findByOperationType(AuditLog criteria);
}
