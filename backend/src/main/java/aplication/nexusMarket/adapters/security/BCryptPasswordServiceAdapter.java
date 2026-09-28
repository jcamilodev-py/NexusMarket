package aplication.nexusMarket.adapters.security;

import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.PasswordServicePort;
import aplication.nexusMarket.domain.valueobjects.Credentials;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Hashes with BCrypt; the Domain only ever sees the resulting hash. */
@Component
public class BCryptPasswordServiceAdapter implements PasswordServicePort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String encode(Credentials credentials) {
        return encoder.encode(credentials.password());
    }

    @Override
    public boolean matches(Credentials credentials, User user) {
        return user != null && user.getPasswordHash() != null
                && encoder.matches(credentials.password(), user.getPasswordHash());
    }
}
