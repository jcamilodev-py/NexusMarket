package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents whether a user can operate normally within the platform.
 *
 * <p>Independent from {@link BuyerCommercialStatus}: a buyer may be an active user of the platform
 * and still be commercially restricted.
 *
 * <p>Source: DOMINIO 1. INACTIVE is inferred to complete the catalog.
 */
@Getter
public enum UserStatus implements DomainCatalog {

    ACTIVE("ACTIVE", "Active",
            "User can access the system normally."),
    INACTIVE("INACTIVE", "Inactive",
            "User exists but cannot perform system operations."),
    BLOCKED("BLOCKED", "Blocked",
            "User access has been suspended.");

    private final String code;
    private final String name;
    private final String description;

    UserStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
