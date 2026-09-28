package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidInventoryException;
import aplication.nexusMarket.domain.valueobjects.InventoryMovementType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single change applied to an inventory record, providing traceability for every stock
 * variation.
 *
 * <p>The movement is the historical record of what happened to the stock, while {@link Inventory}
 * holds the resulting current quantities.
 *
 * <p>Business rules: a movement is immutable once registered - corrections are expressed as new
 * movements of type ADJUSTMENT, never as edits; a movement may never leave the quantities negative;
 * only a Seller (over their own products) or a LogisticsOperator may register one.
 *
 * <p>Source: DOMINIO 6 - "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolucion".
 */
@Getter
@Setter
@NoArgsConstructor
public class InventoryMovement {

    private String identifier;

    private Inventory inventory;

    private InventoryMovementType movementType;

    private Integer quantity;

    /** Date and time the movement occurred. Inferred. */
    private LocalDateTime movementDate;

    /**
     * User whose action produced the movement: a Seller or LogisticsOperator for INBOUND and
     * ADJUSTMENT, the buyer or operator whose order, shipment or return triggered the others.
     */
    private User performedBy;

    /** Positive quantity, except for ADJUSTMENT, which carries the sign of the correction. */
    public static InventoryMovement record(Inventory inventory, InventoryMovementType movementType,
                                           int quantity, User performedBy) {
        if (inventory == null || movementType == null || performedBy == null) {
            throw new InvalidInventoryException("A movement needs its inventory, type and performing user.");
        }
        InventoryMovement movement = new InventoryMovement();
        movement.inventory = inventory;
        movement.movementType = movementType;
        movement.quantity = quantity;
        movement.performedBy = performedBy;
        movement.movementDate = LocalDateTime.now();
        return movement;
    }
}
