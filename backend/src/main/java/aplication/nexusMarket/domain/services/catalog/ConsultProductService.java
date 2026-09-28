package aplication.nexusMarket.domain.services.catalog;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Product;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The owner sees a product in any status; anyone else only once published. An unpublished product is
 * reported as not found, so drafts and suspensions are not disclosed (catalog-services.md).
 */
@Service
@RequiredArgsConstructor
public class ConsultProductService {

    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;

    public Product consultProduct(User requestingUser, Product product) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        if (product == null || product.getIdentifier() == null) {
            throw new EntityNotFoundException("Product");
        }
        Product storedProduct = productRepositoryPort.findById(product)
                .orElseThrow(() -> new EntityNotFoundException("Product"));

        boolean isOwner = consultingUser.hasRole(SystemRole.SELLER)
                && storedProduct.getSeller() != null
                && Objects.equals(consultingUser.getUserId(), storedProduct.getSeller().getUserId());
        if (!isOwner && !storedProduct.isPublished()) {
            throw new EntityNotFoundException("Product");
        }
        return storedProduct;
    }
}
