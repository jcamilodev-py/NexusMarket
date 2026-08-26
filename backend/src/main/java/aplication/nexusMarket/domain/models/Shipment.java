package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.ShipmentStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the logistics process of packing, dispatching and transporting the physical lines of an
 * order to the buyer.
 *
 * <p>An order whose physical lines are stocked in different warehouses produces more than one
 * shipment, since each shipment leaves from exactly one origin warehouse.
 *
 * <p>Business rules: a shipment is created only after the order reaches PAID; it never contains
 * lines referencing a DigitalProduct; every physical order item belongs to exactly one shipment;
 * dispatching generates an InventoryMovement of type SALE_OUTBOUND; the order reaches DELIVERED only
 * when every shipment has been delivered.
 *
 * <p>Source: OBJ-10; Seccion 4.1; Seccion 6.1 steps 7-8; Matriz de Responsabilidades.
 */
@Getter
@Setter
@NoArgsConstructor
public class Shipment {

    private String identifier;

    private Order order;

    /** Order lines included in this shipment. Inferred, to allow partial dispatch. */
    private List<OrderItem> items = new ArrayList<>();

    private Warehouse originWarehouse;

    private LogisticsOperator logisticsOperator;

    private ShipmentStatus shipmentStatus;

    private LocalDateTime dispatchDate;

    /** Confirmation of this date is what advances the order towards DELIVERED. */
    private LocalDateTime deliveryDate;
}
