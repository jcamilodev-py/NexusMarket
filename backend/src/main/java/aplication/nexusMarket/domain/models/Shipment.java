package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidShipmentException;
import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
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
 * it only carries lines whose sourceInventory belongs to its originWarehouse; dispatching generates
 * an InventoryMovement of type SALE_OUTBOUND over each line's sourceInventory; the order reaches
 * DELIVERED only
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

    /**
     * A shipment exists only for a paid order; it may also be created once the order is DISPATCHED,
     * for lines stocked in a warehouse other than the first shipment's.
     */
    public static Shipment prepare(Order order, Warehouse originWarehouse, List<OrderItem> items,
                                   LogisticsOperator logisticsOperator) {
        if (order == null || !(OrderStatus.PAID.equals(order.getOrderStatus())
                || OrderStatus.DISPATCHED.equals(order.getOrderStatus()))) {
            throw new InvalidShipmentException("Shipments exist only for paid orders.");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidShipmentException("The warehouse holds no unshipped physical line of this order.");
        }
        Shipment shipment = new Shipment();
        shipment.order = order;
        shipment.originWarehouse = originWarehouse;
        shipment.items = new ArrayList<>(items);
        shipment.logisticsOperator = logisticsOperator;
        shipment.shipmentStatus = ShipmentStatus.PENDING;
        return shipment;
    }

    public void dispatch() {
        requireStatus("dispatch", ShipmentStatus.PENDING);
        this.shipmentStatus = ShipmentStatus.IN_TRANSIT;
        this.dispatchDate = LocalDateTime.now();
    }

    public void confirmDelivery() {
        requireStatus("confirm the delivery of", ShipmentStatus.IN_TRANSIT);
        this.shipmentStatus = ShipmentStatus.DELIVERED;
        this.deliveryDate = LocalDateTime.now();
    }

    public boolean isDelivered() {
        return ShipmentStatus.DELIVERED.equals(shipmentStatus);
    }

    private void requireStatus(String action, ShipmentStatus expected) {
        if (!expected.equals(shipmentStatus)) {
            throw new InvalidStatusTransitionException("Cannot " + action + " a shipment in status "
                    + (shipmentStatus == null ? "none" : shipmentStatus.getCode()) + ".");
        }
    }
}
