package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidProductException;
import aplication.nexusMarket.domain.exceptions.InvalidStatusTransitionException;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.ProductStatus;
import aplication.nexusMarket.domain.valueobjects.ProductType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

    /** Physical products require inventory and dispatch; digital ones do not (DOMINIO 5). */
    public abstract boolean requiresInventory();

    /**
     * Prepares a product for registration: DRAFT, because Seccion 6.1 registers the product and its
     * stock before publishing it, and typed by its specialization rather than by the client.
     */
    public void initializeDraft(Seller owner) {
        validateDetails(name, price);
        if (currency == null) {
            throw new InvalidProductException("Currency must be provided.");
        }
        if (variants == null || variants.isEmpty()) {
            throw new InvalidProductException("A product must have at least one variant.");
        }
        validateVariants(variants);
        this.seller = owner;
        this.productType = requiresInventory() ? ProductType.PHYSICAL : ProductType.DIGITAL;
        this.productStatus = ProductStatus.DRAFT;
        variants.forEach(variant -> variant.setProduct(this));
    }

    public boolean isPublished() {
        return ProductStatus.PUBLISHED.equals(productStatus);
    }

    public void publish() {
        requireStatus("publish", ProductStatus.DRAFT, ProductStatus.SUSPENDED);
        this.productStatus = ProductStatus.PUBLISHED;
    }

    public void suspend() {
        requireStatus("suspend", ProductStatus.PUBLISHED);
        this.productStatus = ProductStatus.SUSPENDED;
    }

    /** SUSPENDED has a single exit towards PUBLISHED in the documented lifecycle. */
    public void discontinue() {
        requireStatus("discontinue", ProductStatus.DRAFT, ProductStatus.PUBLISHED);
        this.productStatus = ProductStatus.DISCONTINUED;
    }

    /** Returns the attributes that actually changed, so callers can reject a no-op. */
    public List<String> updateDetails(String newName, String newDescription, BigDecimal newPrice) {
        requireNotDiscontinued();
        validateDetails(newName, newPrice);
        List<String> changedFields = new ArrayList<>();
        if (!newName.equals(name)) {
            changedFields.add("name");
        }
        if (!Objects.equals(newDescription, description)) {
            changedFields.add("description");
        }
        if (price == null || newPrice.compareTo(price) != 0) {
            changedFields.add("price");
        }
        this.name = newName;
        this.description = newDescription;
        this.price = newPrice;
        return changedFields;
    }

    /** Existing variants are never edited, since inventory, carts and orders may reference them. */
    public void addVariant(ProductVariant variant) {
        requireNotDiscontinued();
        if (variant == null) {
            throw new InvalidProductException("The variant to add must be provided.");
        }
        List<ProductVariant> candidate = new ArrayList<>(variants);
        candidate.add(variant);
        validateVariants(candidate);
        variant.setProduct(this);
        variants.add(variant);
    }

    private void validateDetails(String candidateName, BigDecimal candidatePrice) {
        if (candidateName == null || candidateName.isBlank()) {
            throw new InvalidProductException("Product name must not be blank.");
        }
        if (candidatePrice == null || candidatePrice.signum() <= 0) {
            throw new InvalidProductException("Product price must be greater than zero.");
        }
    }

    /**
     * Two variants without distinguishing attributes would be the same sellable unit twice, so
     * attributes are optional only for the single default variant.
     */
    private void validateVariants(List<ProductVariant> candidateVariants) {
        Set<String> skus = new HashSet<>();
        Set<String> attributePairs = new HashSet<>();
        for (ProductVariant variant : candidateVariants) {
            if (variant.getSku() == null || variant.getSku().isBlank()) {
                throw new InvalidProductException("Every variant must have a SKU.");
            }
            if (!skus.add(variant.getSku())) {
                throw new InvalidProductException("SKU " + variant.getSku() + " is repeated within the product.");
            }
            boolean hasName = variant.getAttributeName() != null && !variant.getAttributeName().isBlank();
            boolean hasValue = variant.getAttributeValue() != null && !variant.getAttributeValue().isBlank();
            if (hasName != hasValue) {
                throw new InvalidProductException("A variant attribute needs both its name and its value.");
            }
            if (candidateVariants.size() > 1) {
                if (!hasName) {
                    throw new InvalidProductException("Every variant of a product with several variants needs attributes.");
                }
                if (!attributePairs.add(variant.getAttributeName() + "=" + variant.getAttributeValue())) {
                    throw new InvalidProductException("Two variants cannot share the same attribute and value.");
                }
            }
        }
    }

    private void requireStatus(String action, ProductStatus... allowed) {
        if (!Arrays.asList(allowed).contains(productStatus)) {
            throw new InvalidStatusTransitionException("Cannot " + action + " a product in status "
                    + (productStatus == null ? "none" : productStatus.getCode()) + ".");
        }
    }

    private void requireNotDiscontinued() {
        if (ProductStatus.DISCONTINUED.equals(productStatus)) {
            throw new InvalidProductException("A discontinued product can no longer be modified.");
        }
    }
}
