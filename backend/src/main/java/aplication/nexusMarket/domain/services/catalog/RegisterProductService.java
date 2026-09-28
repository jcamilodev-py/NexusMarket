package aplication.nexusMarket.domain.services.catalog;

import aplication.nexusMarket.domain.exceptions.InvalidProductException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Registers a product in DRAFT, owned by the requesting seller: a seller never registers products for
 * another seller (Matriz de Responsabilidades; RG-03) (catalog-services.md - Register Product).
 */
@Service
@RequiredArgsConstructor
public class RegisterProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Product registerProduct(User requestingUser, Product newProduct) {
        User registeringUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(registeringUser, SystemRole.SELLER);
        if (!(registeringUser instanceof Seller seller)) {
            throw new UnauthorizedOperationException("Only a seller may register products.");
        }
        if (newProduct == null) {
            throw new InvalidProductException("The product to register must be provided.");
        }
        newProduct.initializeDraft(seller);
        for (ProductVariant variant : newProduct.getVariants()) {
            if (productRepositoryPort.existsBySku(variant)) {
                throw new InvalidProductException("SKU " + variant.getSku() + " is already used on the platform.");
            }
        }

        Product registeredProduct = productRepositoryPort.save(newProduct);
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.PRODUCT_REGISTRATION, seller,
                        AffectedEntityType.PRODUCT, registeredProduct.getIdentifier()),
                Map.of("productType", registeredProduct.getProductType().getCode(),
                        "variantCount", registeredProduct.getVariants().size()));
        return registeredProduct;
    }
}
