package aplication.nexusMarket.domain.services.operation;

import aplication.nexusMarket.domain.exceptions.InvalidOperationException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.ports.out.OperationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Persists a complete operation in MySQL (operation-audit-services.md - Register Operation). */
@Service
@RequiredArgsConstructor
public class RegisterOperationService {

    private final OperationRepositoryPort operationRepositoryPort;

    public Operation execute(Operation operation) {
        if (operation == null
                || operation.getOperationType() == null
                || operation.getExecutionDate() == null
                || operation.getPerformedBy() == null
                || operation.getAffectedEntityType() == null
                || operation.getAffectedEntityId() == null) {
            throw new InvalidOperationException(
                    "Operation must carry its type, date, performing user and affected entity.");
        }
        return operationRepositoryPort.save(operation);
    }
}
