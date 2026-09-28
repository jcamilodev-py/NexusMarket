package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A product owns its variants, so they are saved and loaded with it. */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductJpaEntity {

    @Id
    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    /** ProductType code. */
    @Column(name = "product_type", nullable = false, length = 40)
    private String productType;

    @Column(name = "seller_id", nullable = false, length = 36)
    private String sellerId;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    /** Currency code. */
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    /** ProductStatus code. */
    @Column(name = "product_status", nullable = false, length = 40)
    private String productStatus;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private List<ProductVariantJpaEntity> variants = new ArrayList<>();
}
