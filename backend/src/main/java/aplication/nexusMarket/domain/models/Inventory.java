package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InsufficientStockException;
import aplication.nexusMarket.domain.exceptions.InvalidInventoryException;
import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.valueobjects.InventoryMovementType;
import aplication.nexusMarket.domain.valueobjects.InventoryStatus;
import java.util.Objects;
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

    /**
     * Opens the single record of a variant in a warehouse. Digital variants never have stock
     * (DOMINIO 5), and a seller warehouse only holds its owner's products.
     */
    public static Inventory open(ProductVariant variant, Warehouse warehouse) {
        if (variant == null || variant.getProduct() == null || warehouse == null) {
            throw new InvalidInventoryException("A variant with its product and a warehouse must be provided.");
        }
        if (!variant.getProduct().requiresInventory()) {
            throw new InvalidInventoryException("Digital products have no inventory.");
        }
        if (warehouse instanceof SellerWarehouse sellerWarehouse
                && (sellerWarehouse.getOwner() == null || variant.getProduct().getSeller() == null
                || !Objects.equals(sellerWarehouse.getOwner().getUserId(), variant.getProduct().getSeller().getUserId()))) {
            throw new InvalidInventoryException("A seller warehouse only holds stock of its owner's products.");
        }
        Inventory inventory = new Inventory();
        inventory.variant = variant;
        inventory.warehouse = warehouse;
        inventory.availableQuantity = 0;
        inventory.reservedQuantity = 0;
        inventory.inventoryStatus = InventoryStatus.AVAILABLE;
        return inventory;
    }

    public InventoryMovement receive(int quantity, User performedBy) {
        requirePositive(quantity);
        availableQuantity += quantity;
        return InventoryMovement.record(this, InventoryMovementType.INBOUND, quantity, performedBy);
    }

    /** The movement carries the signed delta; reserved units belong to orders and are never adjusted. */
    public InventoryMovement adjust(int delta, User performedBy) {
        if (delta == 0) {
            throw new InvalidInventoryException("An adjustment must change the quantity.");
        }
        if (availableQuantity + delta < 0) {
            throw new InsufficientStockException("The adjustment would leave the available quantity negative.");
        }
        availableQuantity += delta;
        return InventoryMovement.record(this, InventoryMovementType.ADJUSTMENT, delta, performedBy);
    }

    /** Damaged or insufficient stock cannot be reserved (Seccion 11). */
    public boolean canReserve(int quantity) {
        return InventoryStatus.AVAILABLE.equals(inventoryStatus) && quantity > 0 && availableQuantity >= quantity;
    }

    public InventoryMovement reserve(int quantity, User performedBy) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException("This inventory record cannot cover the reservation.");
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
        return InventoryMovement.record(this, InventoryMovementType.RESERVATION, quantity, performedBy);
    }

    public InventoryMovement releaseReservation(int quantity, User performedBy) {
        requireReserved(quantity);
        reservedQuantity -= quantity;
        availableQuantity += quantity;
        return InventoryMovement.record(this, InventoryMovementType.RESERVATION_RELEASE, quantity, performedBy);
    }

    public InventoryMovement registerSaleOutbound(int quantity, User performedBy) {
        requireReserved(quantity);
        reservedQuantity -= quantity;
        return InventoryMovement.record(this, InventoryMovementType.SALE_OUTBOUND, quantity, performedBy);
    }

    public InventoryMovement registerReturn(int quantity, User performedBy) {
        requirePositive(quantity);
        availableQuantity += quantity;
        return InventoryMovement.record(this, InventoryMovementType.RETURN, quantity, performedBy);
    }

    /** Moves no units, so it produces no movement; units already reserved are unaffected. */
    public void changeStatus(InventoryStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidInventoryException("Target inventory status must be provided.");
        }
        if (newStatus.equals(inventoryStatus)) {
            throw new InvalidStatusTransitionException("Inventory already has status " + newStatus.getCode() + ".");
        }
        this.inventoryStatus = newStatus;
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidInventoryException("Quantity must be greater than zero.");
        }
    }

    private void requireReserved(int quantity) {
        requirePositive(quantity);
        if (reservedQuantity < quantity) {
            throw new InsufficientStockException("Not enough reserved units.");
        }
    }
}
