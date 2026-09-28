package aplication.nexusMarket.adapters.persistence.mongodb.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.mongodb.documents.AuditLogDocument;
import aplication.nexusMarket.domain.models.Administrator;
import aplication.nexusMarket.domain.models.AuditLog;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.LogisticsOperator;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.Supervisor;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.HashMap;

/**
 * Reading rebuilds the performer as a reference of the specialization given by the stored role,
 * carrying only what the document holds: identifier, name and role.
 */
public final class AuditLogMongoMapper {

    private AuditLogMongoMapper() {
    }

    public static AuditLogDocument toDocument(AuditLog domain) {
        AuditLogDocument document = new AuditLogDocument();
        document.setAuditId(domain.getAuditId());
        document.setOperationType(CatalogCodes.code(domain.getOperationType()));
        document.setOperationDate(domain.getOperationDate());
        document.setPerformedByUserId(domain.getPerformedBy().getUserId());
        document.setPerformedByFullName(domain.getPerformedBy().getFullName());
        document.setUserRole(CatalogCodes.code(domain.getUserRole()));
        document.setAffectedEntityType(CatalogCodes.code(domain.getAffectedEntityType()));
        document.setAffectedEntityId(domain.getAffectedEntityId());
        document.setDetails(new HashMap<>(domain.getDetails()));
        return document;
    }

    public static AuditLog toDomain(AuditLogDocument document) {
        SystemRole role = CatalogCodes.fromCode(SystemRole.class, document.getUserRole());
        AuditLog domain = new AuditLog();
        domain.setAuditId(document.getAuditId());
        domain.setOperationType(CatalogCodes.fromCode(OperationType.class, document.getOperationType()));
        domain.setOperationDate(document.getOperationDate());
        domain.setPerformedBy(performerReference(role, document));
        domain.setUserRole(role);
        domain.setAffectedEntityType(CatalogCodes.fromCode(AffectedEntityType.class, document.getAffectedEntityType()));
        domain.setAffectedEntityId(document.getAffectedEntityId());
        domain.setDetails(document.getDetails() == null ? new HashMap<>() : new HashMap<>(document.getDetails()));
        return domain;
    }

    private static User performerReference(SystemRole role, AuditLogDocument document) {
        User performer = switch (role) {
            case BUYER -> new Buyer();
            case SELLER -> new Seller();
            case LOGISTICS_OPERATOR -> new LogisticsOperator();
            case ADMINISTRATOR -> new Administrator();
            case SUPERVISOR -> new Supervisor();
        };
        performer.setUserId(document.getPerformedByUserId());
        performer.setFullName(document.getPerformedByFullName());
        performer.setRole(role);
        return performer;
    }
}
