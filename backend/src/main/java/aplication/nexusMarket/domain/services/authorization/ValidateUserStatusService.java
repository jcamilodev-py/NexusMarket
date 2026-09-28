package aplication.nexusMarket.domain.services.authorization;

import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Returns the authoritative stored user when it exists and is ACTIVE (RG-01).
 *
 * <p>The status is read from storage, never from the user rebuilt from the token, so a user blocked
 * after logging in is stopped immediately (authorization-services.md - Validate User Status).
 */
@Service
@RequiredArgsConstructor
public class ValidateUserStatusService {

    private final UserRepositoryPort userRepositoryPort;

    public User execute(User requestingUser) {
        if (requestingUser == null || requestingUser.getUserId() == null) {
            throw new UnauthorizedOperationException("Requesting user must be provided.");
        }
        User storedUser = userRepositoryPort.findById(requestingUser)
                .orElseThrow(() -> new UnauthorizedOperationException("Requesting user is not registered."));
        if (!storedUser.isActive()) {
            throw new UnauthorizedOperationException("Requesting user is not active.");
        }
        return storedUser;
    }
}
