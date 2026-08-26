package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the condition of a stock record, used to determine whether it is eligible for
 * reservation.
 *
 * <p>Source: Seccion 11 - stock that does not exist or is marked as damaged cannot be reserved.
 * AVAILABLE is inferred as the necessary counterpart to DAMAGED.
 */
@Getter
public enum InventoryStatus implements DomainCatalog {

    AVAILABLE("AVAILABLE", "Available",
            "Stock can be normally reserved and sold."),
    DAMAGED("DAMAGED", "Damaged",
            "Stock is marked as damaged and cannot be reserved.");

    private final String code;
    private final String name;
    private final String description;

    InventoryStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
