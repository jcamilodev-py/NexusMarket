package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidInventoryException;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.OrderItem;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryMovementRepositoryPort;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Internal: removes the reserved units of a dispatched line from the record that reserved them. */
@Service
@RequiredArgsConstructor
public class RegisterSaleOutboundService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Inventory execute(User performedBy, OrderItem orderItem) {
        if (orderItem == null || orderItem.getSourceInventory() == null || orderItem.getQuantity() == null) {
            throw new InvalidInventoryException("The order line must carry its source inventory and quantity.");
        }
        Inventory inventory = inventoryRepositoryPort.findById(orderItem.getSourceInventory())
                .orElseThrow(() -> new EntityNotFoundException("Inventory"));

        InventoryMovement movement = inventory.registerSaleOutbound(orderItem.getQuantity(), performedBy);
        inventoryRepositoryPort.update(inventory);
        inventoryMovementRepositoryPort.save(movement);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.INVENTORY_SALE_OUTBOUND, performedBy,
                        AffectedEntityType.INVENTORY, inventory.getIdentifier()),
                ReserveInventoryService.orderDetails(orderItem, orderItem.getQuantity()));
        return inventory;
    }
}
