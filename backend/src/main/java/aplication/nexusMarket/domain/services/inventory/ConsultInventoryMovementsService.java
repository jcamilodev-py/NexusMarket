package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryMovementRepositoryPort;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Movement history of an inventory record, for the same participants as Consult Inventory. */
@Service
@RequiredArgsConstructor
public class ConsultInventoryMovementsService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public List<InventoryMovement> consultInventoryMovements(User requestingUser, Inventory inventory) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.SELLER,
                SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (inventory == null || inventory.getIdentifier() == null) {
            throw new EntityNotFoundException("Inventory");
        }
        Inventory storedInventory = inventoryRepositoryPort.findById(inventory)
                .orElseThrow(() -> new EntityNotFoundException("Inventory"));
        if (consultingUser.hasRole(SystemRole.SELLER)) {
            validateSellerOwnershipService.execute(consultingUser, storedInventory.getVariant().getProduct().getSeller());
        }
        return inventoryMovementRepositoryPort.findByInventory(storedInventory);
    }
}
