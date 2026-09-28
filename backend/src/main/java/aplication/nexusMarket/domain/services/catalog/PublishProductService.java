package aplication.nexusMarket.domain.services.catalog;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidProductException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.ProductStatus;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes a draft or suspended product. A physical product needs an inventory record for every
 * variant first, because Seccion 6.1 registers stock (step 3) before publication (step 4); a record
 * suffices, even with its stock momentarily at zero (catalog-services.md - Publish Product).
 */
@Service
@RequiredArgsConstructor
public class PublishProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Product publishProduct(User requestingUser, Product product) {
        User seller = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(seller, SystemRole.SELLER);
        if (product == null || product.getIdentifier() == null) {
            throw new EntityNotFoundException("Product");
        }
        Product storedProduct = productRepositoryPort.findById(product)
                .orElseThrow(() -> new EntityNotFoundException("Product"));
        validateSellerOwnershipService.execute(seller, storedProduct.getSeller());

        if (storedProduct.requiresInventory()
                && !storedProduct.getVariants().stream().allMatch(inventoryRepositoryPort::existsByVariant)) {
            throw new InvalidProductException("Every variant of a physical product needs inventory before publication.");
        }
        ProductStatus previousStatus = storedProduct.getProductStatus();
        storedProduct.publish();
        productRepositoryPort.update(storedProduct);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.PRODUCT_PUBLICATION, seller,
                        AffectedEntityType.PRODUCT, storedProduct.getIdentifier()),
                Map.of("previousStatus", previousStatus.getCode()));
        return storedProduct;
    }
}
