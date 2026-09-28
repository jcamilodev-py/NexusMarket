package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidInventoryException;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryMovementRepositoryPort;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Corrects the available quantity with a signed ADJUSTMENT movement; earlier movements are never
 * edited and reserved units are never adjusted (inventory-services.md - Adjust Inventory).
 */
@Service
@RequiredArgsConstructor
public class AdjustInventoryService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Inventory adjustInventory(User requestingUser, InventoryMovement adjustment) {
        User adjustingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(adjustingUser, SystemRole.SELLER, SystemRole.LOGISTICS_OPERATOR);
        if (adjustment == null || adjustment.getQuantity() == null || adjustment.getInventory() == null
                || adjustment.getInventory().getIdentifier() == null) {
            throw new InvalidInventoryException("The adjustment must carry its quantity and inventory.");
        }
        Inventory inventory = inventoryRepositoryPort.findById(adjustment.getInventory())
                .orElseThrow(() -> new EntityNotFoundException("Inventory"));
        if (adjustingUser.hasRole(SystemRole.SELLER)) {
            validateSellerOwnershipService.execute(adjustingUser, inventory.getVariant().getProduct().getSeller());
        }

        InventoryMovement movement = inventory.adjust(adjustment.getQuantity(), adjustingUser);
        inventoryRepositoryPort.update(inventory);
        inventoryMovementRepositoryPort.save(movement);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.INVENTORY_ADJUSTMENT, adjustingUser,
                        AffectedEntityType.INVENTORY, inventory.getIdentifier()),
                Map.of("quantity", movement.getQuantity(), "availableAfter", inventory.getAvailableQuantity()));
        return inventory;
    }
}
