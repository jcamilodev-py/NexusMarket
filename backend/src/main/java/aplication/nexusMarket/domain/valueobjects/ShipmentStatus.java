package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the current execution stage of a shipment.
 *
 * <p>A single order may produce several shipments when its physical lines are stocked in different
 * warehouses, and each of them carries its own status.
 *
 * <p>Source: OBJ-10; Seccion 6.1 step 7. Values inferred by mapping onto the packing, dispatch and
 * transport sequence described by the specification.
 */
@Getter
public enum ShipmentStatus implements DomainCatalog {

    PENDING("PENDING", "Pending",
            "Shipment created and being packed; it has not yet left the warehouse."),
    IN_TRANSIT("IN_TRANSIT", "In Transit",
            "Shipment dispatched from the warehouse and en route to the buyer."),
    DELIVERED("DELIVERED", "Delivered",
            "Delivery confirmed. When every shipment of an order reaches this state, the order "
                    + "moves to DELIVERED.");

    private final String code;
    private final String name;
    private final String description;

    ShipmentStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
