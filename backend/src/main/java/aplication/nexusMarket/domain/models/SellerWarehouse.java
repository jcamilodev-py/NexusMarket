package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a warehouse owned by a specific seller.
 *
 * <p>Every seller is registered together with their first warehouse during onboarding, which makes
 * the ownership relationship mandatory in both directions.
 *
 * <p>Source: DOMINIO 4; Seccion 6.1 step 1.
 */
@Getter
@Setter
@NoArgsConstructor
public class SellerWarehouse extends Warehouse {

    /** Seller who owns this warehouse. Mandatory. */
    private Seller owner;
}
