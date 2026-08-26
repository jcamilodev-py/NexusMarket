package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.BuyerCommercialStatus;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a registered customer who purchases products published on the marketplace.
 *
 * <p>Holds the delivery addresses used to fulfil orders and a commercial status that determines
 * whether purchases may currently be placed. A buyer never manages information belonging to other
 * buyers or to inventory.
 *
 * <p>Business rules: a buyer has exactly one active cart at any given time; a buyer whose
 * commercialStatus is not ENABLED cannot confirm an order.
 *
 * <p>Source: DOMINIO 2; OBJ-03; Matriz de Responsabilidades.
 */
@Getter
@Setter
@NoArgsConstructor
public class Buyer extends User {

    private String primaryAddress;

    /** Secondary delivery locations. Optional; empty by default. */
    private List<String> additionalAddresses = new ArrayList<>();

    private BuyerCommercialStatus commercialStatus;

    /** The single open cart of the buyer. Null when no selection is in progress. */
    private Cart activeCart;

    /** Not populated by default; loaded on demand by the corresponding consultation service. */
    private List<Order> orders = new ArrayList<>();
}
