package aplication.nexusMarket.domain.services.catalog;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.User;
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
 * Permanently removes a product from sale; existing orders, invoices and returns are unaffected
 * (catalog-services.md - Discontinue Product).
 */
@Service
@RequiredArgsConstructor
public class DiscontinueProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Product discontinueProduct(User requestingUser, Product product) {
        User seller = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(seller, SystemRole.SELLER);
        if (product == null || product.getIdentifier() == null) {
            throw new EntityNotFoundException("Product");
        }
        Product storedProduct = productRepositoryPort.findById(product)
                .orElseThrow(() -> new EntityNotFoundException("Product"));
        validateSellerOwnershipService.execute(seller, storedProduct.getSeller());

        ProductStatus previousStatus = storedProduct.getProductStatus();
        storedProduct.discontinue();
        productRepositoryPort.update(storedProduct);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.PRODUCT_DISCONTINUATION, seller,
                        AffectedEntityType.PRODUCT, storedProduct.getIdentifier()),
                Map.of("previousStatus", previousStatus.getCode()));
        return storedProduct;
    }
}
