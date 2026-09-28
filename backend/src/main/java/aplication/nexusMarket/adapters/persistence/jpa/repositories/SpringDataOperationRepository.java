package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.OperationJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOperationRepository extends JpaRepository<OperationJpaEntity, String> {

    List<OperationJpaEntity> findByPerformedById(String performedById);

    List<OperationJpaEntity> findByAffectedEntityTypeAndAffectedEntityId(String affectedEntityType, String affectedEntityId);
}
