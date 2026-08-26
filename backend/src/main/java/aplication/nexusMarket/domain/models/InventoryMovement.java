package aplication.nexusMarket.domain.models;

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

    /** User who triggered the movement: a Seller or a LogisticsOperator. Inferred. */
    private User performedBy;
}
