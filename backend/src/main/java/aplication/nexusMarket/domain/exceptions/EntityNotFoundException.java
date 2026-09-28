package aplication.nexusMarket.domain.exceptions;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName) {
        super(entityName + " not found.");
    }
}
