package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidInventoryException;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.models.Warehouse;
import aplication.nexusMarket.domain.ports.out.InventoryMovementRepositoryPort;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.ports.out.WarehouseRepositoryPort;
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
 * Registers stock entering a warehouse, opening the variant's record there on its first inbound
 * (Seccion 6.1 step 3). Sellers act only on their own products; logistics operators on any
 * (Matriz de Responsabilidades) (inventory-services.md - Register Inventory Inbound).
 */
@Service
@RequiredArgsConstructor
public class RegisterInventoryInboundService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Inventory registerInventoryInbound(User requestingUser, InventoryMovement inbound) {
        User receivingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(receivingUser, SystemRole.SELLER, SystemRole.LOGISTICS_OPERATOR);
        if (inbound == null || inbound.getQuantity() == null || inbound.getInventory() == null
                || inbound.getInventory().getVariant() == null || inbound.getInventory().getWarehouse() == null) {
            throw new InvalidInventoryException("The inbound must carry its quantity, variant and warehouse.");
        }
        ProductVariant variant = productRepositoryPort.findVariantById(inbound.getInventory().getVariant())
                .orElseThrow(() -> new EntityNotFoundException("Product variant"));
        Warehouse warehouse = warehouseRepositoryPort.findById(inbound.getInventory().getWarehouse())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse"));
        if (receivingUser.hasRole(SystemRole.SELLER)) {
            validateSellerOwnershipService.execute(receivingUser, variant.getProduct().getSeller());
        }

        Inventory criteria = new Inventory();
        criteria.setVariant(variant);
        criteria.setWarehouse(warehouse);
        Inventory inventory = inventoryRepositoryPort.findByVariantAndWarehouse(criteria)
                .orElseGet(() -> inventoryRepositoryPort.save(Inventory.open(variant, warehouse)));

        InventoryMovement movement = inventory.receive(inbound.getQuantity(), receivingUser);
        inventoryRepositoryPort.update(inventory);
        inventoryMovementRepositoryPort.save(movement);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.INVENTORY_INBOUND, receivingUser,
                        AffectedEntityType.INVENTORY, inventory.getIdentifier()),
                Map.of("quantity", movement.getQuantity(), "sku", variant.getSku(),
                        "warehouseId", warehouse.getIdentifier(), "availableAfter", inventory.getAvailableQuantity()));
        return inventory;
    }
}
