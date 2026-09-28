package aplication.nexusMarket.domain.services.catalog;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidProductException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Updates name, description and price, and adds the variants received without variantId. Existing
 * variants are never touched, since inventory, carts and orders may reference them
 * (catalog-services.md - Update Product).
 */
@Service
@RequiredArgsConstructor
public class UpdateProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Product updateProduct(User requestingUser, Product product) {
        User seller = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(seller, SystemRole.SELLER);
        if (product == null || product.getIdentifier() == null) {
            throw new EntityNotFoundException("Product");
        }
        Product storedProduct = productRepositoryPort.findById(product)
                .orElseThrow(() -> new EntityNotFoundException("Product"));
        validateSellerOwnershipService.execute(seller, storedProduct.getSeller());

        List<String> changedFields = storedProduct.updateDetails(
                product.getName(), product.getDescription(), product.getPrice());
        List<ProductVariant> newVariants = product.getVariants() == null ? List.of()
                : product.getVariants().stream().filter(variant -> variant.getVariantId() == null).toList();
        for (ProductVariant variant : newVariants) {
            if (productRepositoryPort.existsBySku(variant)) {
                throw new InvalidProductException("SKU " + variant.getSku() + " is already used on the platform.");
            }
            storedProduct.addVariant(variant);
        }
        if (changedFields.isEmpty() && newVariants.isEmpty()) {
            throw new InvalidProductException("The update does not change the product.");
        }
        productRepositoryPort.update(storedProduct);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.PRODUCT_UPDATE, seller,
                        AffectedEntityType.PRODUCT, storedProduct.getIdentifier()),
                Map.of("changedFields", changedFields, "addedVariantCount", newVariants.size()));
        return storedProduct;
    }
}
