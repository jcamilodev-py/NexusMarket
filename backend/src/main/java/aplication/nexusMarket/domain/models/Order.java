package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the formal commercial commitment of the buyer, created once a {@link Cart} is confirmed
 * at checkout. Its lifecycle is the central process of the system.
 *
 * <p>An order freezes the commercial conditions of the purchase - the variants selected, their
 * quantities, their unit prices and the delivery address - so later changes to the catalog or to the
 * buyer profile never alter historical orders.
 *
 * <p>Business rules: an order is created only from a confirmed cart, by a buyer whose
 * commercialStatus is ENABLED; an order in DELIVERED can no longer be modified (Seccion 11); an
 * order may reach CANCELLED from PENDING_PAYMENT or PAID, never after dispatch, and cancelling
 * releases every inventory reservation it holds.
 *
 * <p>Mixed orders: digital lines are delivered as soon as the order reaches PAID; physical lines
 * generate one or more shipments; the order reaches DISPATCHED when its first shipment is
 * dispatched and DELIVERED only when every shipment has been delivered; a digital-only order
 * generates no shipment and moves directly from PAID to DELIVERED.
 *
 * <p>Source: DOMINIO 7; OBJ-08; Seccion 6.1 steps 5-8; Seccion 11.
 */
@Getter
@Setter
@NoArgsConstructor
public class Order {

    private String identifier;

    private Buyer buyer;

    /** Lines confirmed within the order, copied from the cart. At least one element. */
    private List<OrderItem> orderItems = new ArrayList<>();

    /** Delivery address chosen for this order, copied at checkout. Inferred. */
    private String shippingAddress;

    private OrderStatus orderStatus;

    private LocalDateTime creationDate;

    /** Sum of the subtotals of the order items, computed at creation and never recalculated. */
    private BigDecimal totalAmount;

    /** Currency in which totalAmount is expressed. Inferred. */
    private Currency currency;

    /** Payment attempts registered against the order. */
    private List<Payment> payments = new ArrayList<>();

    /** Issued once the order reaches PAID. Null before that. */
    private Invoice invoice;

    /** Empty for orders composed exclusively of digital products. */
    private List<Shipment> shipments = new ArrayList<>();
}
