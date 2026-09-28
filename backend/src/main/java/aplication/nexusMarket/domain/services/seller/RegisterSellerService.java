package aplication.nexusMarket.domain.services.seller;

import aplication.nexusMarket.domain.exceptions.InvalidCredentialsException;
import aplication.nexusMarket.domain.exceptions.InvalidUserException;
import aplication.nexusMarket.domain.exceptions.InvalidWarehouseException;
import aplication.nexusMarket.domain.exceptions.UserAlreadyExistsException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.SellerWarehouse;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.PasswordServicePort;
import aplication.nexusMarket.domain.ports.out.SellerRepositoryPort;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import aplication.nexusMarket.domain.ports.out.WarehouseRepositoryPort;
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
 * Onboards a seller together with their first warehouse, as Seccion 6.1 step 1 requires; only an
 * Administrator may do it, since sellers cannot self-register (DOMINIO 3).
 *
 * <p>Two operations are registered, one per entity created, so the warehouse is traceable from the
 * moment it exists (seller-services.md - Register Seller).
 */
@Service
@RequiredArgsConstructor
public class RegisterSellerService {

    private final SellerRepositoryPort sellerRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordServicePort passwordServicePort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Seller registerSeller(User requestingUser, Seller newSeller, SellerWarehouse firstWarehouse,
                                 Credentials credentials) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);

        if (newSeller == null) {
            throw new InvalidUserException("The seller to register must be provided.");
        }
        newSeller.validateIdentity();
        newSeller.validateCommercialIdentity();
        if (firstWarehouse == null) {
            throw new InvalidWarehouseException("The seller's first warehouse must be provided.");
        }
        firstWarehouse.validateAddress();
        if (credentials == null || !credentials.email().equals(newSeller.getEmail())) {
            throw new InvalidCredentialsException();
        }
        if (userRepositoryPort.existsByEmail(newSeller)) {
            throw new UserAlreadyExistsException("A user with this email is already registered.");
        }
        if (userRepositoryPort.existsByIdentificationNumber(newSeller)) {
            throw new UserAlreadyExistsException("A user with this identification number is already registered.");
        }

        newSeller.assignPasswordHash(passwordServicePort.encode(credentials));
        newSeller.setRole(SystemRole.SELLER);
        newSeller.setStatus(UserStatus.ACTIVE);
        Seller registeredSeller = sellerRepositoryPort.save(newSeller);

        registeredSeller.addWarehouse(firstWarehouse);
        SellerWarehouse registeredWarehouse = warehouseRepositoryPort.save(firstWarehouse);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.SELLER_REGISTRATION, administrator,
                        AffectedEntityType.SELLER, registeredSeller.getUserId()),
                Map.of("firstWarehouseId", registeredWarehouse.getIdentifier()));
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.WAREHOUSE_REGISTRATION, administrator,
                        AffectedEntityType.WAREHOUSE, registeredWarehouse.getIdentifier()),
                Map.of("warehouseType", "SELLER", "ownerId", registeredSeller.getUserId()));
        return registeredSeller;
    }
}
