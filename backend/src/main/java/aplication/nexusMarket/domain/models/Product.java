package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.ProductStatus;
import aplication.nexusMarket.domain.valueobjects.ProductType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a good offered for sale in the marketplace catalog.
 *
 * <p>A product is a <b>catalog concept</b>: it carries the commercial description that buyers
 * browse. It is not itself the sellable unit, which is {@link ProductVariant} - the entity that
 * inventory, carts and orders actually reference.
 *
 * <p>Business rules: every product has at least one variant, so a product with no real variation is
 * registered with a single default variant; all variants share the product price; only a product in
 * state PUBLISHED is visible in the public catalog.
 *
 * <p>Source: DOMINIO 5; OBJ-05; Seccion 6.1 steps 2 and 4.
 */
@Getter
@Setter
public abstract class Product {

    private String identifier;

    /** Commercial name of the product. Inferred. */
    private String name;

    /** Descriptive text of the product. Inferred. */
    private String description;

    /** Physical or Digital, kept alongside the subclass so the catalog can be filtered by value. */
    private ProductType productType;

    /** Seller who registered and owns the product. */
    private Seller seller;

    /** Sale price of the product. Inferred. */
    private BigDecimal price;

    /** Currency in which the price is expressed. Inferred. */
    private Currency currency;

    /** Sellable variations. Always holds at least one element. */
    private List<ProductVariant> variants = new ArrayList<>();

    private ProductStatus productStatus;
}
