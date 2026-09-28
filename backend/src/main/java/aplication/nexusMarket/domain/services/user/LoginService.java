package aplication.nexusMarket.domain.services.user;

import aplication.nexusMarket.domain.exceptions.InvalidCredentialsException;
import aplication.nexusMarket.domain.exceptions.InvalidUserStatusException;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.JwtServicePort;
import aplication.nexusMarket.domain.ports.out.PasswordServicePort;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import aplication.nexusMarket.domain.valueobjects.Credentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Authenticates a user and returns their token (user-authentication-services.md - Login).
 *
 * <p>Unknown email and wrong password fail identically, and the status is checked only after the
 * password, so a failed login never reveals whether an email is registered.
 */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordServicePort passwordServicePort;
    private final JwtServicePort jwtServicePort;

    public String login(Credentials credentials) {
        if (credentials == null) {
            throw new InvalidCredentialsException();
        }
        User storedUser = userRepositoryPort.findByEmail(credentials)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordServicePort.matches(credentials, storedUser)) {
            throw new InvalidCredentialsException();
        }
        if (!storedUser.isActive()) {
            throw new InvalidUserStatusException("User is not active and cannot authenticate.");
        }
        return jwtServicePort.generateToken(storedUser);
    }
}
