package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.entities.ProductJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.ProductJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataProductRepository;
import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.valueobjects.ProductStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Products with their variants; a variant is always returned inside its own product. */
@Repository
@RequiredArgsConstructor
public class ProductJpaAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository productRepository;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public <T extends Product> T save(T product) {
        assignIdentifiers(product);
        productRepository.save(ProductJpaMapper.toEntity(product));
        return product;
    }

    @Override
    public Optional<Product> findById(Product product) {
        if (product == null || product.getIdentifier() == null) {
            return Optional.empty();
        }
        return productRepository.findById(product.getIdentifier()).map(this::toDomain);
    }

    @Override
    public List<Product> findPublished() {
        return productRepository.findByProductStatus(CatalogCodes.code(ProductStatus.PUBLISHED)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ProductVariant> findVariantById(ProductVariant variant) {
        if (variant == null || variant.getVariantId() == null) {
            return Optional.empty();
        }
        return productRepository.findByVariants_VariantId(variant.getVariantId())
                .map(this::toDomain)
                .flatMap(product -> product.getVariants().stream()
                        .filter(candidate -> variant.getVariantId().equals(candidate.getVariantId()))
                        .findFirst());
    }

    @Override
    public boolean existsBySku(ProductVariant variant) {
        return variant != null && variant.getSku() != null && productRepository.existsByVariants_Sku(variant.getSku());
    }

    @Override
    public void update(Product product) {
        assignIdentifiers(product);
        productRepository.save(ProductJpaMapper.toEntity(product));
    }

    /** Variants added by Update Product arrive without identifier. */
    private void assignIdentifiers(Product product) {
        product.setIdentifier(Identifiers.orNew(product.getIdentifier()));
        product.getVariants().forEach(variant -> variant.setVariantId(Identifiers.orNew(variant.getVariantId())));
    }

    private Product toDomain(ProductJpaEntity entity) {
        return ProductJpaMapper.toDomain(entity, referenceResolver.user(entity.getSellerId(), Seller.class));
    }
}
