package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the current stage of a monetary refund.
 *
 * <p>{@code Refund} is the financial counterpart of {@code Payment}: one records money leaving the
 * marketplace, the other money entering it.
 *
 * <p>Source: OBJ-11; Matriz de Responsabilidades. Values inferred.
 */
@Getter
public enum RefundStatus implements DomainCatalog {

    PENDING("PENDING", "Pending",
            "Refund originated by an approved return, not yet executed."),
    PROCESSED("PROCESSED", "Processed",
            "Funds returned to the buyer by an administrator."),
    REJECTED("REJECTED", "Rejected",
            "Refund denied during administrative review.");

    private final String code;
    private final String name;
    private final String description;

    RefundStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
