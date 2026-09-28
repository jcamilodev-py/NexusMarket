package aplication.nexusMarket.domain.services.warehouse;

import aplication.nexusMarket.domain.exceptions.InvalidWarehouseException;
import aplication.nexusMarket.domain.models.MarketplaceWarehouse;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
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

/** Registers a warehouse owned by NexusMarket itself (DOMINIO 4; Seccion 5). */
@Service
@RequiredArgsConstructor
public class RegisterMarketplaceWarehouseService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public MarketplaceWarehouse registerMarketplaceWarehouse(User requestingUser, MarketplaceWarehouse newWarehouse) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (newWarehouse == null) {
            throw new InvalidWarehouseException("The warehouse to register must be provided.");
        }
        newWarehouse.validateAddress();

        MarketplaceWarehouse registeredWarehouse = warehouseRepositoryPort.save(newWarehouse);
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.WAREHOUSE_REGISTRATION, administrator,
                        AffectedEntityType.WAREHOUSE, registeredWarehouse.getIdentifier()),
                Map.of("warehouseType", "MARKETPLACE"));
        return registeredWarehouse;
    }
}
