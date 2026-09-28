package aplication.nexusMarket.domain.services.warehouse;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.models.Warehouse;
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
 * Updates a warehouse's address. The owner of a seller warehouse never changes: the specification
 * describes no transfer of warehouses between sellers (warehouse-services.md - Update Warehouse).
 */
@Service
@RequiredArgsConstructor
public class UpdateWarehouseService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Warehouse updateWarehouse(User requestingUser, Warehouse warehouse) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (warehouse == null || warehouse.getIdentifier() == null) {
            throw new EntityNotFoundException("Warehouse");
        }
        Warehouse storedWarehouse = warehouseRepositoryPort.findById(warehouse)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse"));

        String previousAddress = storedWarehouse.getAddress();
        storedWarehouse.relocate(warehouse.getAddress());
        warehouseRepositoryPort.update(storedWarehouse);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.WAREHOUSE_UPDATE, administrator,
                        AffectedEntityType.WAREHOUSE, storedWarehouse.getIdentifier()),
                Map.of("previousAddress", previousAddress, "newAddress", storedWarehouse.getAddress()));
        return storedWarehouse;
    }
}
