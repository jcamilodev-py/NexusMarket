package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.InsufficientStockException;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Internal: reserves one physical order line in full from a single record, the one with the most
 * available units, and keeps it as the line's sourceInventory so dispatch and returns use the same
 * warehouse (inventory-services.md - Reserve Inventory).
 */
@Service
@RequiredArgsConstructor
public class ReserveInventoryService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final InventoryMovementRepositoryPort inventoryMovementRepositoryPort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Inventory execute(User performedBy, OrderItem orderItem) {
        if (orderItem == null || orderItem.getVariant() == null || orderItem.getQuantity() == null) {
            throw new InvalidInventoryException("The order line to reserve must carry its variant and quantity.");
        }
        int quantity = orderItem.getQuantity();
        Inventory inventory = inventoryRepositoryPort.findByVariant(orderItem.getVariant()).stream()
                .filter(candidate -> candidate.canReserve(quantity))
                .max(Comparator.comparingInt(Inventory::getAvailableQuantity))
                .orElseThrow(() -> new InsufficientStockException(
                        "No single warehouse can cover " + quantity + " units of " + orderItem.getVariant().getSku() + "."));

        InventoryMovement movement = inventory.reserve(quantity, performedBy);
        orderItem.setSourceInventory(inventory);
        inventoryRepositoryPort.update(inventory);
        inventoryMovementRepositoryPort.save(movement);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.INVENTORY_RESERVATION, performedBy,
                        AffectedEntityType.INVENTORY, inventory.getIdentifier()),
                orderDetails(orderItem, quantity));
        return inventory;
    }

    static Map<String, Object> orderDetails(OrderItem orderItem, int quantity) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", orderItem.getOrder() == null ? null : orderItem.getOrder().getIdentifier());
        details.put("quantity", quantity);
        return details;
    }
}
