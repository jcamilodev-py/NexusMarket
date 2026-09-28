package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import java.util.List;

/** Offers no update nor delete: movements are immutable. */
public interface InventoryMovementRepositoryPort {

    InventoryMovement save(InventoryMovement movement);

    List<InventoryMovement> findByInventory(Inventory inventory);
}
