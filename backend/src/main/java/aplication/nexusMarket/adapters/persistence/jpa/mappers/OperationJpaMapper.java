package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.OperationJpaEntity;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;

public final class OperationJpaMapper {

    private OperationJpaMapper() {
    }

    public static OperationJpaEntity toEntity(Operation domain) {
        OperationJpaEntity entity = new OperationJpaEntity();
        entity.setOperationId(domain.getOperationId());
        entity.setOperationType(CatalogCodes.code(domain.getOperationType()));
        entity.setExecutionDate(domain.getExecutionDate());
        entity.setPerformedById(domain.getPerformedBy().getUserId());
        entity.setAffectedEntityType(CatalogCodes.code(domain.getAffectedEntityType()));
        entity.setAffectedEntityId(domain.getAffectedEntityId());
        return entity;
    }

    public static Operation toDomain(OperationJpaEntity entity, User performedBy) {
        Operation domain = new Operation();
        domain.setOperationId(entity.getOperationId());
        domain.setOperationType(CatalogCodes.fromCode(OperationType.class, entity.getOperationType()));
        domain.setExecutionDate(entity.getExecutionDate());
        domain.setPerformedBy(performedBy);
        domain.setAffectedEntityType(CatalogCodes.fromCode(AffectedEntityType.class, entity.getAffectedEntityType()));
        domain.setAffectedEntityId(entity.getAffectedEntityId());
        return domain;
    }
}
