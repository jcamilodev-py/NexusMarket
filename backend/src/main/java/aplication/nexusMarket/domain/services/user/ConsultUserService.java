package aplication.nexusMarket.domain.services.user;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Retrieves a user for themself, an Administrator or a Supervisor (RG-03; Seccion 5).
 *
 * <p>Access is checked before the lookup, so a user without permission cannot learn whether another
 * user exists (user-authentication-services.md - Consult User).
 */
@Service
@RequiredArgsConstructor
public class ConsultUserService {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;

    public User consultUser(User requestingUser, User user) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        if (user == null || user.getUserId() == null) {
            throw new EntityNotFoundException("User");
        }
        boolean consultsThemself = Objects.equals(consultingUser.getUserId(), user.getUserId());
        if (!consultsThemself
                && !consultingUser.hasRole(SystemRole.ADMINISTRATOR)
                && !consultingUser.hasRole(SystemRole.SUPERVISOR)) {
            throw new UnauthorizedOperationException("Only the user, an administrator or a supervisor may consult this user.");
        }
        return userRepositoryPort.findById(user)
                .orElseThrow(() -> new EntityNotFoundException("User"));
    }
}
