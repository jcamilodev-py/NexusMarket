package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidBuyerException;
import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.valueobjects.BuyerCommercialStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    /** A RESTRICTED buyer keeps platform access but cannot place orders (DOMINIO 2). */
    public boolean canPlaceOrders() {
        return BuyerCommercialStatus.ENABLED.equals(commercialStatus);
    }

    /** Copies the list, so later changes by the caller never reach the buyer's profile. */
    public void updateAddresses(String newPrimaryAddress, List<String> newAdditionalAddresses) {
        if (newPrimaryAddress == null || newPrimaryAddress.isBlank()) {
            throw new InvalidBuyerException("Primary address must not be blank.");
        }
        List<String> additional = newAdditionalAddresses == null ? List.of() : newAdditionalAddresses;
        if (additional.stream().anyMatch(address -> address == null || address.isBlank())) {
            throw new InvalidBuyerException("Additional addresses must not be blank.");
        }
        this.primaryAddress = newPrimaryAddress;
        this.additionalAddresses = new ArrayList<>(additional);
    }

    public void changeCommercialStatus(BuyerCommercialStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidBuyerException("Target commercial status must be provided.");
        }
        if (newStatus.equals(commercialStatus)) {
            throw new InvalidStatusTransitionException(
                    "Buyer already has commercial status " + newStatus.getCode() + ".");
        }
        this.commercialStatus = newStatus;
    }

    public void assignActiveCart(Cart cart) {
        if (cart == null || cart.getBuyer() == null
                || !Objects.equals(cart.getBuyer().getUserId(), getUserId())) {
            throw new InvalidBuyerException("The active cart must belong to the buyer.");
        }
        this.activeCart = cart;
    }
}
