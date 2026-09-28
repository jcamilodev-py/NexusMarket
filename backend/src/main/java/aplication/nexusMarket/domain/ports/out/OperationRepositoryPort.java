package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import java.util.List;

public interface OperationRepositoryPort {

    Operation save(Operation operation);

    List<Operation> findByPerformedBy(User user);

    List<Operation> findByAffectedEntity(Operation criteria);
}
