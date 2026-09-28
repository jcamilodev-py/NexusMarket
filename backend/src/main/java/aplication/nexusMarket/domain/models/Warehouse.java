package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidWarehouseException;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a physical storage location used to manage inventory.
 *
 * <p>Inventory is always tied to exactly one warehouse (DOMINIO 6), which makes warehouses
 * individually identifiable and addressable places.
 *
 * <p>Source: DOMINIO 4; OBJ-04; DOMINIO 6.
 */
@Getter
@Setter
public abstract class Warehouse {

    private String identifier;

    /** Physical location of the warehouse. Inferred. */
    private String address;

    /** A storage location is meaningless without a physical location. */
    public void validateAddress() {
        if (address == null || address.isBlank()) {
            throw new InvalidWarehouseException("Warehouse address must not be blank.");
        }
    }

    /** Rejects an unchanged address, which would record a relocation that did not happen. */
    public void relocate(String newAddress) {
        if (newAddress == null || newAddress.isBlank()) {
            throw new InvalidWarehouseException("Warehouse address must not be blank.");
        }
        if (newAddress.equals(address)) {
            throw new InvalidWarehouseException("The warehouse is already at this address.");
        }
        this.address = newAddress;
    }
}
