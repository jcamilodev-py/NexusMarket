package aplication.nexusMarket.domain.services.user;

import aplication.nexusMarket.domain.exceptions.InvalidCredentialsException;
import aplication.nexusMarket.domain.exceptions.InvalidUserException;
import aplication.nexusMarket.domain.exceptions.UserAlreadyExistsException;
import aplication.nexusMarket.domain.models.Administrator;
import aplication.nexusMarket.domain.models.LogisticsOperator;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Supervisor;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.PasswordServicePort;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.Credentials;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import aplication.nexusMarket.domain.valueobjects.UserStatus;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Registers a logistics operator, administrator or supervisor. Buyers and sellers are registered by
 * their own subdomains, since their registration creates more than a user.
 *
 * <p>Executed by an Administrator (inferred from OBJ-01 and Seccion 5); identity document and email
 * must be unique (Seccion 11) (user-authentication-services.md - Register Staff User).
 */
@Service
@RequiredArgsConstructor
public class RegisterStaffUserService {

    private static final Map<SystemRole, Class<? extends User>> STAFF_SPECIALIZATIONS = Map.of(
            SystemRole.LOGISTICS_OPERATOR, LogisticsOperator.class,
            SystemRole.ADMINISTRATOR, Administrator.class,
            SystemRole.SUPERVISOR, Supervisor.class);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordServicePort passwordServicePort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public User registerStaffUser(User requestingUser, User newUser, Credentials credentials) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);

        validateUserInformation(newUser);
        validateStaffRole(newUser);
        validateCredentials(newUser, credentials);
        validateUniqueness(newUser);

        newUser.assignPasswordHash(passwordServicePort.encode(credentials));
        newUser.setStatus(UserStatus.ACTIVE);
        User registeredUser = userRepositoryPort.save(newUser);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.USER_REGISTRATION, administrator,
                        AffectedEntityType.USER, registeredUser.getUserId()),
                Map.of("role", registeredUser.getRole().getCode()));
        return registeredUser;
    }

    private void validateUserInformation(User newUser) {
        if (newUser == null) {
            throw new InvalidUserException("The user to register must be provided.");
        }
        if (isBlank(newUser.getFullName())) {
            throw new InvalidUserException("Full name must not be blank.");
        }
        if (isBlank(newUser.getIdentificationNumber())) {
            throw new InvalidUserException("Identification number must not be blank.");
        }
        if (isBlank(newUser.getEmail())) {
            throw new InvalidUserException("Email must not be blank.");
        }
    }

    private void validateStaffRole(User newUser) {
        Class<? extends User> expectedSpecialization = STAFF_SPECIALIZATIONS.get(newUser.getRole());
        if (expectedSpecialization == null) {
            throw new InvalidUserException("Only logistics operators, administrators and supervisors are registered here.");
        }
        if (!expectedSpecialization.isInstance(newUser)) {
            throw new InvalidUserException("The role does not match the kind of user being registered.");
        }
    }

    private void validateCredentials(User newUser, Credentials credentials) {
        if (credentials == null || !credentials.email().equals(newUser.getEmail())) {
            throw new InvalidCredentialsException();
        }
    }

    private void validateUniqueness(User newUser) {
        if (userRepositoryPort.existsByEmail(newUser)) {
            throw new UserAlreadyExistsException("A user with this email is already registered.");
        }
        if (userRepositoryPort.existsByIdentificationNumber(newUser)) {
            throw new UserAlreadyExistsException("A user with this identification number is already registered.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
