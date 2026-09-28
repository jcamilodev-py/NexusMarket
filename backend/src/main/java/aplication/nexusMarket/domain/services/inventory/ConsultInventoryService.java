package aplication.nexusMarket.domain.services.inventory;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Stock of a variant per warehouse. Buyers never consult inventory (DOMINIO 2). */
@Service
@RequiredArgsConstructor
public class ConsultInventoryService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public List<Inventory> consultInventory(User requestingUser, ProductVariant variant) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.SELLER,
                SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (variant == null || variant.getVariantId() == null) {
            throw new EntityNotFoundException("Product variant");
        }
        ProductVariant storedVariant = productRepositoryPort.findVariantById(variant)
                .orElseThrow(() -> new EntityNotFoundException("Product variant"));
        if (consultingUser.hasRole(SystemRole.SELLER)) {
            validateSellerOwnershipService.execute(consultingUser, storedVariant.getProduct().getSeller());
        }
        return inventoryRepositoryPort.findByVariant(storedVariant);
    }
}
