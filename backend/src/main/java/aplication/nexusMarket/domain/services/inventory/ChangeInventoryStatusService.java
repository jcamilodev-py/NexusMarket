package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.InventoryStatus;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Marks stock DAMAGED or AVAILABLE. Damaged stock cannot be reserved (Seccion 11); units already
 * reserved from it stay with their orders (inventory-services.md - Change Inventory Status).
 */
@Service
@RequiredArgsConstructor
public class ChangeInventoryStatusService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Inventory changeInventoryStatus(User requestingUser, Inventory inventory) {
        User changingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(changingUser, SystemRole.SELLER, SystemRole.LOGISTICS_OPERATOR);
        if (inventory == null || inventory.getIdentifier() == null) {
            throw new EntityNotFoundException("Inventory");
        }
        Inventory storedInventory = inventoryRepositoryPort.findById(inventory)
                .orElseThrow(() -> new EntityNotFoundException("Inventory"));
        if (changingUser.hasRole(SystemRole.SELLER)) {
            validateSellerOwnershipService.execute(changingUser, storedInventory.getVariant().getProduct().getSeller());
        }

        InventoryStatus previousStatus = storedInventory.getInventoryStatus();
        storedInventory.changeStatus(inventory.getInventoryStatus());
        inventoryRepositoryPort.update(storedInventory);

        Map<String, Object> details = new HashMap<>();
        details.put("previousStatus", previousStatus == null ? null : previousStatus.getCode());
        details.put("newStatus", storedInventory.getInventoryStatus().getCode());
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.INVENTORY_STATUS_CHANGE, changingUser,
                        AffectedEntityType.INVENTORY, storedInventory.getIdentifier()),
                details);
        return storedInventory;
    }
}
