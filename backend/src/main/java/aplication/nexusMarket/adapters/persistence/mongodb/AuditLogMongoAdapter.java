package aplication.nexusMarket.adapters.persistence.mongodb;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.mongodb.mappers.AuditLogMongoMapper;
import aplication.nexusMarket.adapters.persistence.mongodb.repositories.AuditLogMongoRepository;
import aplication.nexusMarket.domain.models.AuditLog;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.AuditLogRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** The audit trail in MongoDB (nexusmarket_audit.audit_logs). Append-only: no update, no delete. */
@Repository
@RequiredArgsConstructor
public class AuditLogMongoAdapter implements AuditLogRepositoryPort {

    private final AuditLogMongoRepository auditLogRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        auditLog.setAuditId(Identifiers.orNew(auditLog.getAuditId()));
        auditLogRepository.save(AuditLogMongoMapper.toDocument(auditLog));
        return auditLog;
    }

    @Override
    public List<AuditLog> findByPerformedBy(User user) {
        return auditLogRepository.findByPerformedByUserId(user.getUserId()).stream()
                .map(AuditLogMongoMapper::toDomain)
                .toList();
    }

    @Override
    public List<AuditLog> findByAffectedEntity(AuditLog criteria) {
        return auditLogRepository.findByAffectedEntityTypeAndAffectedEntityId(
                        CatalogCodes.code(criteria.getAffectedEntityType()), criteria.getAffectedEntityId()).stream()
                .map(AuditLogMongoMapper::toDomain)
                .toList();
    }

    @Override
    public List<AuditLog> findByOperationType(AuditLog criteria) {
        return auditLogRepository.findByOperationType(CatalogCodes.code(criteria.getOperationType())).stream()
                .map(AuditLogMongoMapper::toDomain)
                .toList();
    }
}
