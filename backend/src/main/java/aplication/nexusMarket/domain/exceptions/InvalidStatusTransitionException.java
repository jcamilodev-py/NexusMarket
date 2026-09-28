package aplication.nexusMarket.domain.exceptions;

public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
