package aplication.nexusMarket.domain.valueobjects;

import aplication.nexusMarket.domain.exceptions.InvalidCredentialsException;

/**
 * Represents the proof of identity a participant presents to authenticate: the email that identifies
 * them and the password that proves it.
 *
 * <p>The only Value Object of the domain that is not a catalog. It is the input of Login and of the
 * registration services, because {@code User} is abstract and its specialization is unknown at login.
 * It is never persisted; the password is hashed or compared through an output port and discarded.
 *
 * <p>Source: DOMINIO 1 ("Medio principal de acceso"); RG-01.
 */
public record Credentials(String email, String password) {

    public Credentials {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }
    }

    /** Overridden so the password never reaches a log through string conversion. */
    @Override
    public String toString() {
        return "Credentials[email=" + email + ", password=****]";
    }
}
