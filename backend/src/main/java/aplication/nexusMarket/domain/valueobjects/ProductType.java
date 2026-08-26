package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents whether a product is physical or digital, determining whether it requires inventory
 * and shipping.
 *
 * <p>Carried as an attribute of {@code Product} in addition to the PhysicalProduct / DigitalProduct
 * specialization, so the catalog can be filtered and validated without relying on the runtime type.
 *
 * <p>Source: DOMINIO 5 - "Tipo de Producto: Fisico o Digital".
 */
@Getter
public enum ProductType implements DomainCatalog {

    PHYSICAL("PHYSICAL", "Physical",
            "Requires inventory tracking and physical dispatch through a warehouse."),
    DIGITAL("DIGITAL", "Digital",
            "Delivered immediately upon payment confirmation.");

    private final String code;
    private final String name;
    private final String description;

    ProductType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
