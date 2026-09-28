package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Its own table because the variant has identity: inventory, carts and orders reference it. */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariantJpaEntity {

    @Id
    @Column(name = "variant_id", length = 36)
    private String variantId;

    @Column(name = "sku", nullable = false, unique = true, length = 80)
    private String sku;

    @Column(name = "attribute_name", length = 80)
    private String attributeName;

    @Column(name = "attribute_value", length = 80)
    private String attributeValue;
}
