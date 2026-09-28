package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidInvoiceException;
import aplication.nexusMarket.domain.exceptions.InvalidOrderException;
import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
 * order may reach CANCELLED only from PENDING_PAYMENT, because once paid the only reimbursement
 * path is a refund originated by a return (OBJ-11), and cancelling releases every inventory
 * reservation it holds.
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

    /**
     * Converts a confirmed cart into an order, freezing each unit price and the delivery address so
     * later catalog or profile changes never alter it. The address must be one of the buyer's own.
     */
    public static Order placeFrom(Cart cart, String shippingAddress) {
        if (cart == null || cart.isEmpty()) {
            throw new InvalidOrderException("An order needs a cart with at least one line.");
        }
        Buyer buyer = cart.getBuyer();
        boolean ownAddress = shippingAddress != null && !shippingAddress.isBlank()
                && (shippingAddress.equals(buyer.getPrimaryAddress()) || buyer.getAdditionalAddresses().contains(shippingAddress));
        if (!ownAddress) {
            throw new InvalidOrderException("The shipping address must be one of the buyer's addresses.");
        }
        Order order = new Order();
        order.buyer = buyer;
        order.shippingAddress = shippingAddress;
        for (CartItem line : cart.getCartItems()) {
            Product product = line.getVariant().getProduct();
            if (!product.isPublished()) {
                throw new InvalidOrderException("Product " + product.getName() + " is no longer published.");
            }
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setVariant(line.getVariant());
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            order.orderItems.add(item);
        }
        order.totalAmount = order.orderItems.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.currency = cart.getCartItems().get(0).getVariant().getProduct().getCurrency();
        order.orderStatus = OrderStatus.PENDING_PAYMENT;
        order.creationDate = LocalDateTime.now();
        return order;
    }

    public boolean hasPhysicalItems() {
        return !physicalItems().isEmpty();
    }

    /**
     * Lines reserved from the warehouse and not yet in a shipment. A line is identified by its variant,
     * since the cart merges each variant into a single line.
     */
    public List<OrderItem> unshippedPhysicalItemsFrom(Warehouse warehouse) {
        return physicalItems().stream()
                .filter(item -> item.getSourceInventory() != null && warehouse != null
                        && Objects.equals(item.getSourceInventory().getWarehouse().getIdentifier(), warehouse.getIdentifier()))
                .filter(item -> shipments.stream().noneMatch(shipment -> containsLine(shipment, item)))
                .toList();
    }

    public boolean containsProductsOf(User seller) {
        return seller != null && orderItems.stream()
                .map(item -> item.getVariant().getProduct().getSeller())
                .anyMatch(owner -> owner != null && Objects.equals(owner.getUserId(), seller.getUserId()));
    }

    /** Only an unpaid order can be cancelled: after payment the only reimbursement path is a return. */
    public void cancel() {
        requireStatus("cancel", OrderStatus.PENDING_PAYMENT);
        this.orderStatus = OrderStatus.CANCELLED;
    }

    public void confirmPayment() {
        requireStatus("confirm the payment of", OrderStatus.PENDING_PAYMENT);
        this.orderStatus = OrderStatus.PAID;
    }

    public void markDispatched() {
        requireStatus("dispatch", OrderStatus.PAID);
        if (!hasPhysicalItems()) {
            throw new InvalidStatusTransitionException("An order without physical lines is never dispatched.");
        }
        this.orderStatus = OrderStatus.DISPATCHED;
    }

    /** A digital-only order is ready once paid; any other, once every physical line is delivered. */
    public boolean isReadyForDelivery() {
        if (OrderStatus.PAID.equals(orderStatus)) {
            return !hasPhysicalItems();
        }
        return OrderStatus.DISPATCHED.equals(orderStatus)
                && physicalItems().stream().allMatch(item -> shipments.stream()
                        .anyMatch(shipment -> shipment.isDelivered() && containsLine(shipment, item)));
    }

    /** DELIVERED has no outgoing transition, which is how Seccion 11 keeps a finished order unchanged. */
    public void markDelivered() {
        if (!isReadyForDelivery()) {
            throw new InvalidStatusTransitionException("The order is not ready to be marked as delivered.");
        }
        this.orderStatus = OrderStatus.DELIVERED;
    }

    public void registerPaymentAttempt(Payment payment) {
        payments.add(payment);
    }

    public void attachInvoice(Invoice newInvoice) {
        if (invoice != null) {
            throw new InvalidInvoiceException("The order already has an invoice.");
        }
        this.invoice = newInvoice;
    }

    public void addShipment(Shipment shipment) {
        shipments.add(shipment);
    }

    private List<OrderItem> physicalItems() {
        return orderItems.stream().filter(item -> item.getVariant().getProduct().requiresInventory()).toList();
    }

    private static boolean containsLine(Shipment shipment, OrderItem item) {
        return shipment.getItems().stream()
                .anyMatch(line -> Objects.equals(line.getVariant().getVariantId(), item.getVariant().getVariantId()));
    }

    private void requireStatus(String action, OrderStatus expected) {
        if (!expected.equals(orderStatus)) {
            throw new InvalidStatusTransitionException("Cannot " + action + " an order in status "
                    + (orderStatus == null ? "none" : orderStatus.getCode()) + ".");
        }
    }
}
