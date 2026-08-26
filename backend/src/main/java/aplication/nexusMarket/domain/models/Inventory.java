package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.InventoryStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the stock of a specific product variant within a specific warehouse.
 *
 * <p>Inventory is distributed: it is always linked to exactly one variant and one warehouse. The
 * association targets {@link ProductVariant} rather than {@link Product} because stock counted at
 * product level cannot answer how many units of "red, size M" remain in a given warehouse.
 *
 * <p>Business rules: quantities must never be negative; stock that does not exist or is DAMAGED
 * cannot be reserved; an Inventory record exists only for variants of a PhysicalProduct; every
 * change to the quantities must be recorded as an {@link InventoryMovement}.
 *
 * <p>Source: DOMINIO 6; OBJ-06; Seccion 11.
 */
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    private String identifier;

    /** Product variant this record refers to. Mandatory. */
    private ProductVariant variant;

    /** Warehouse this record belongs to. Mandatory. */
    private Warehouse warehouse;

    /** Quantity currently available for sale. Must never be negative. */
    private Integer availableQuantity;

    /** Quantity reserved by pending orders. Inferred from the RESERVATION movement type. */
    private Integer reservedQuantity;

    private InventoryStatus inventoryStatus;
}
