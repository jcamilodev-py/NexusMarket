package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.ProductJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import aplication.nexusMarket.domain.models.DigitalProduct;
import aplication.nexusMarket.domain.models.PhysicalProduct;
import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.ProductStatus;
import aplication.nexusMarket.domain.valueobjects.ProductType;
import java.util.ArrayList;

/** The stored product type decides the specialization; every variant points back to its product. */
public final class ProductJpaMapper {

    private ProductJpaMapper() {
    }

    public static ProductJpaEntity toEntity(Product domain) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setProductId(domain.getIdentifier());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setProductType(CatalogCodes.code(domain.getProductType()));
        entity.setSellerId(domain.getSeller() == null ? null : domain.getSeller().getUserId());
        entity.setPrice(domain.getPrice());
        entity.setCurrency(CatalogCodes.code(domain.getCurrency()));
        entity.setProductStatus(CatalogCodes.code(domain.getProductStatus()));
        entity.setVariants(new ArrayList<>(domain.getVariants().stream().map(ProductJpaMapper::toEntity).toList()));
        return entity;
    }

    public static Product toDomain(ProductJpaEntity entity, Seller seller) {
        ProductType type = CatalogCodes.fromCode(ProductType.class, entity.getProductType());
        Product domain = ProductType.PHYSICAL.equals(type) ? new PhysicalProduct() : new DigitalProduct();
        domain.setIdentifier(entity.getProductId());
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setProductType(type);
        domain.setSeller(seller);
        domain.setPrice(entity.getPrice());
        domain.setCurrency(CatalogCodes.fromCode(Currency.class, entity.getCurrency()));
        domain.setProductStatus(CatalogCodes.fromCode(ProductStatus.class, entity.getProductStatus()));
        domain.setVariants(new ArrayList<>(entity.getVariants().stream()
                .map(variantEntity -> toDomain(variantEntity, domain))
                .toList()));
        return domain;
    }

    private static ProductVariantJpaEntity toEntity(ProductVariant variant) {
        ProductVariantJpaEntity entity = new ProductVariantJpaEntity();
        entity.setVariantId(variant.getVariantId());
        entity.setSku(variant.getSku());
        entity.setAttributeName(variant.getAttributeName());
        entity.setAttributeValue(variant.getAttributeValue());
        return entity;
    }

    private static ProductVariant toDomain(ProductVariantJpaEntity entity, Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(entity.getVariantId());
        variant.setSku(entity.getSku());
        variant.setProduct(product);
        variant.setAttributeName(entity.getAttributeName());
        variant.setAttributeValue(entity.getAttributeValue());
        return variant;
    }
}
