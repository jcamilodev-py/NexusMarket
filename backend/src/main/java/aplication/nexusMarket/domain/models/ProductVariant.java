package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a specific sellable variation of a product, such as a colour, a size or a model.
 *
 * <p>The variant is the unit that the business actually sells and counts: stock is held per variant,
 * and cart and order lines record the exact variant selected by the buyer. Modeling it as an
 * attribute of {@link Product} would make it impossible to track stock for a concrete combination
 * such as "red, size M", which the distributed inventory of DOMINIO 6 requires.
 *
 * <p>The generic attributeName/attributeValue pair covers any variant dimension implied by the
 * specification without requiring a model change.
 *
 * <p>Source: DOMINIO 5 - "Variantes: Diferencias de color, talla, modelo, etc."; DOMINIO 6.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    /** Internal unique identifier of the variant. Inferred. */
    private String variantId;

    /** Unique commercial code of the sellable unit (Stock Keeping Unit). Inferred. */
    private String sku;

    private Product product;

    /** Name of the varying characteristic, for example "Color" or "Talla". Inferred. */
    private String attributeName;

    /** Value of that characteristic, for example "Rojo" or "M". */
    private String attributeValue;
}
