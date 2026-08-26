package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a warehouse owned and operated directly by NexusMarket.
 *
 * <p>Has no owning seller; it belongs to the marketplace itself, and may hold inventory for variants
 * belonging to any seller.
 *
 * <p>Source: DOMINIO 4 - classification of marketplace versus seller warehouses.
 */
@Getter
@Setter
@NoArgsConstructor
public class MarketplaceWarehouse extends Warehouse {
}
