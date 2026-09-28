package aplication.nexusMarket.domain.exceptions;

public class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
