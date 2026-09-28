package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.entities.OperationJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.OperationJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataOperationRepository;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OperationRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Append-only: an operation records an event, and the event never changes. */
@Repository
@RequiredArgsConstructor
public class OperationJpaAdapter implements OperationRepositoryPort {

    private final SpringDataOperationRepository operationRepository;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public Operation save(Operation operation) {
        operation.setOperationId(Identifiers.orNew(operation.getOperationId()));
        operationRepository.save(OperationJpaMapper.toEntity(operation));
        return operation;
    }

    @Override
    public List<Operation> findByPerformedBy(User user) {
        return toDomain(operationRepository.findByPerformedById(user.getUserId()));
    }

    @Override
    public List<Operation> findByAffectedEntity(Operation criteria) {
        return toDomain(operationRepository.findByAffectedEntityTypeAndAffectedEntityId(
                CatalogCodes.code(criteria.getAffectedEntityType()), criteria.getAffectedEntityId()));
    }

    private List<Operation> toDomain(List<OperationJpaEntity> entities) {
        return entities.stream()
                .map(entity -> OperationJpaMapper.toDomain(entity, referenceResolver.user(entity.getPerformedById())))
                .toList();
    }
}
