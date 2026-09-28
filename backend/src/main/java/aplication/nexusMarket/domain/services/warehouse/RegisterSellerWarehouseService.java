package aplication.nexusMarket.domain.services.warehouse;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidWarehouseException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.SellerWarehouse;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.SellerRepositoryPort;
import aplication.nexusMarket.domain.ports.out.WarehouseRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Registers an additional warehouse for an existing seller; the first one comes with Register Seller.
 *
 * <p>The owner is replaced by the stored seller through Seller.addWarehouse, so the warehouse never
 * points at the partial Seller sent by the client (warehouse-services.md - Register Seller Warehouse).
 */
@Service
@RequiredArgsConstructor
public class RegisterSellerWarehouseService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final SellerRepositoryPort sellerRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public SellerWarehouse registerSellerWarehouse(User requestingUser, SellerWarehouse newWarehouse) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (newWarehouse == null) {
            throw new InvalidWarehouseException("The warehouse to register must be provided.");
        }
        newWarehouse.validateAddress();
        if (newWarehouse.getOwner() == null || newWarehouse.getOwner().getUserId() == null) {
            throw new EntityNotFoundException("Seller");
        }
        Seller owner = sellerRepositoryPort.findById(newWarehouse.getOwner())
                .orElseThrow(() -> new EntityNotFoundException("Seller"));

        owner.addWarehouse(newWarehouse);
        SellerWarehouse registeredWarehouse = warehouseRepositoryPort.save(newWarehouse);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.WAREHOUSE_REGISTRATION, administrator,
                        AffectedEntityType.WAREHOUSE, registeredWarehouse.getIdentifier()),
                Map.of("warehouseType", "SELLER", "ownerId", owner.getUserId()));
        return registeredWarehouse;
    }
}
