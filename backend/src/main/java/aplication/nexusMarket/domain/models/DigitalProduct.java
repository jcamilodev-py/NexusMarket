package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an intangible product delivered immediately upon payment confirmation, without
 * warehouse or shipment involvement.
 *
 * <p>Business rules: a digital product never generates Inventory records nor InventoryMovement
 * records, never participates in a Shipment, and its order lines are considered delivered as soon as
 * the order reaches PAID.
 *
 * <p>No additional attributes: the specification states only the delivery timing rule and not a
 * delivery mechanism, so none is modeled.
 *
 * <p>Source: DOMINIO 5 - digital products are delivered immediately after payment.
 */
@Getter
@Setter
@NoArgsConstructor
public class DigitalProduct extends Product {
}
