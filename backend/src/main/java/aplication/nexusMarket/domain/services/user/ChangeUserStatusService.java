package aplication.nexusMarket.domain.services.user;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidUserStatusException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import aplication.nexusMarket.domain.valueobjects.UserStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Changes the status of a user; the target status travels inside the supplied User.
 *
 * <p>An Administrator cannot change their own status, so the platform can never be left without an
 * active Administrator able to reverse it (inferred). BuyerCommercialStatus is never touched
 * (user-authentication-services.md - Change User Status).
 */
@Service
@RequiredArgsConstructor
public class ChangeUserStatusService {

    private final UserRepositoryPort userRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public User changeUserStatus(User requestingUser, User user) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (user == null || user.getStatus() == null) {
            throw new InvalidUserStatusException("Target user status must be provided.");
        }

        User storedUser = userRepositoryPort.findById(user)
                .orElseThrow(() -> new EntityNotFoundException("User"));
        if (Objects.equals(administrator.getUserId(), storedUser.getUserId())) {
            throw new UnauthorizedOperationException("An administrator cannot change their own status.");
        }

        UserStatus previousStatus = storedUser.getStatus();
        storedUser.changeStatus(user.getStatus());
        userRepositoryPort.update(storedUser);

        Map<String, Object> details = new HashMap<>();
        details.put("previousStatus", previousStatus == null ? null : previousStatus.getCode());
        details.put("newStatus", storedUser.getStatus().getCode());
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.USER_STATUS_CHANGE, administrator,
                        AffectedEntityType.USER, storedUser.getUserId()),
                details);
        return storedUser;
    }
}
