package aplication.nexusMarket.domain.models;

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
}
