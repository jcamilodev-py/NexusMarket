package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a tangible product that requires inventory tracking and physical dispatch through a
 * warehouse.
 *
 * <p>No additional attributes: the distinction from {@link DigitalProduct} is behavioural. Only the
 * variants of a physical product participate in {@link Inventory}, and only its order lines
 * participate in {@link Shipment}.
 *
 * <p>Source: DOMINIO 5 - physical products require inventory and dispatch.
 */
@Getter
@Setter
@NoArgsConstructor
public class PhysicalProduct extends Product {
}
