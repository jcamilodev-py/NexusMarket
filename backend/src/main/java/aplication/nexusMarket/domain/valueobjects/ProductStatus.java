package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the publication state of a product in the catalog.
 *
 * <p>Only a product in state PUBLISHED is visible in the public catalog and may be added to a cart.
 * Every product is registered in DRAFT. SUSPENDED is reversible; DISCONTINUED is terminal.
 *
 * <p>Lifecycle:
 *
 * <pre>
 * DRAFT -&gt; PUBLISHED &lt;-&gt; SUSPENDED
 *   |          |
 *   |          +-&gt; DISCONTINUED
 *   +-&gt; DISCONTINUED
 * </pre>
 *
 * <p>Source: DOMINIO 5 - "Estado: Publicado, Suspendido o Descontinuado". DRAFT is inferred:
 * Seccion 6.1 registers the product (step 2) and its inventory (step 3) before publishing it
 * (step 4), and none of the literal values describes a product not yet published.
 */
@Getter
public enum ProductStatus implements DomainCatalog {

    DRAFT("DRAFT", "Draft",
            "Registered by its seller but not yet published."),
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
