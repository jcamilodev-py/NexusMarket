package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidInventoryException;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.ReturnItem;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryMovementRepositoryPort;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Internal: puts returned units back into the record the line was sold from (DOMINIO 6 "Devolucion"). */
@Service
@RequiredArgsConstructor
public class RegisterInventoryReturnService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Inventory execute(User performedBy, ReturnItem returnItem) {
        if (returnItem == null || returnItem.getQuantity() == null || returnItem.getOrderItem() == null
                || returnItem.getOrderItem().getSourceInventory() == null) {
            throw new InvalidInventoryException("The returned line must carry its quantity and source inventory.");
        }
        Inventory inventory = inventoryRepositoryPort.findById(returnItem.getOrderItem().getSourceInventory())
                .orElseThrow(() -> new EntityNotFoundException("Inventory"));

        InventoryMovement movement = inventory.registerReturn(returnItem.getQuantity(), performedBy);
        inventoryRepositoryPort.update(inventory);
        inventoryMovementRepositoryPort.save(movement);

        Map<String, Object> details = new HashMap<>();
        details.put("returnRequestId", returnItem.getReturnRequest() == null ? null : returnItem.getReturnRequest().getIdentifier());
        details.put("quantity", returnItem.getQuantity());
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.INVENTORY_RETURN, performedBy,
                        AffectedEntityType.INVENTORY, inventory.getIdentifier()),
                details);
        return inventory;
    }
}
