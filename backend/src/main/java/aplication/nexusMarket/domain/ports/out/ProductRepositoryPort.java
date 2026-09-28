package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.ProductVariant;
import java.util.List;
import java.util.Optional;

/** save assigns product and variant identifiers and returns the specialization it received. */
public interface ProductRepositoryPort {

    <T extends Product> T save(T product);

    Optional<Product> findById(Product product);

    List<Product> findPublished();

    /** Returns the variant with its product and the product's seller. */
    Optional<ProductVariant> findVariantById(ProductVariant variant);

    boolean existsBySku(ProductVariant variant);

    void update(Product product);
}
