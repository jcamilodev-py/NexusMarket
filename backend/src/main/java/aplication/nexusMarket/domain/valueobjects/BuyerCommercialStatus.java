package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the buyer's condition for placing purchases.
 *
 * <p>Independent from {@link UserStatus}: a buyer whose commercial status is not ENABLED cannot
 * confirm an order, even though their platform access remains active.
 *
 * <p>Source: DOMINIO 2. Values inferred; the specification names the attribute without enumerating
 * its values.
 */
@Getter
public enum BuyerCommercialStatus implements DomainCatalog {

    ENABLED("ENABLED", "Enabled",
            "Buyer can place orders normally."),
    RESTRICTED("RESTRICTED", "Restricted",
            "Buyer is temporarily prevented from purchasing.");

    private final String code;
    private final String name;
    private final String description;

    BuyerCommercialStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
