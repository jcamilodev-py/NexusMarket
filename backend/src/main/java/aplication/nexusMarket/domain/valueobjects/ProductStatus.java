package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the publication state of a product in the catalog.
 *
 * <p>Only a product in state PUBLISHED is visible in the public catalog and may be added to a cart.
 * SUSPENDED is reversible; DISCONTINUED is terminal.
 *
 * <p>Source: DOMINIO 5 - "Estado: Publicado, Suspendido o Descontinuado".
 */
@Getter
public enum ProductStatus implements DomainCatalog {

    PUBLISHED("PUBLISHED", "Published",
            "Visible in the public catalog."),
    SUSPENDED("SUSPENDED", "Suspended",
            "Temporarily hidden from the catalog."),
    DISCONTINUED("DISCONTINUED", "Discontinued",
            "Permanently removed from active sale.");

    private final String code;
    private final String name;
    private final String description;

    ProductStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
