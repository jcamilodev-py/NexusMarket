package aplication.nexusMarket.domain.services.authorization;

import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Accepts the user only when their single role is one of the allowed roles (RG-02, RG-03). */
@Service
@RequiredArgsConstructor
public class ValidateRolePermissionService {

    public void execute(User user, SystemRole... allowedRoles) {
        if (user == null || Arrays.stream(allowedRoles).noneMatch(user::hasRole)) {
            throw new UnauthorizedOperationException("The user's role is not allowed to perform this operation.");
        }
    }
}
