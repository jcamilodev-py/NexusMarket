package aplication.nexusMarket.domain.exceptions;

/**
 * Carries one fixed message for every credential failure, so a failed login never reveals whether
 * the email is registered.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid credentials.");
    }
}
