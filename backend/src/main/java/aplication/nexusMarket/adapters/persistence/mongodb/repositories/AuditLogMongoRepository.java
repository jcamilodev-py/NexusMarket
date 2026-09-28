package aplication.nexusMarket.adapters.persistence.mongodb.repositories;

import aplication.nexusMarket.adapters.persistence.mongodb.documents.AuditLogDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String> {

    List<AuditLogDocument> findByPerformedByUserId(String performedByUserId);

    List<AuditLogDocument> findByAffectedEntityTypeAndAffectedEntityId(String affectedEntityType, String affectedEntityId);

    List<AuditLogDocument> findByOperationType(String operationType);
}
