package aplication.nexusMarket.domain.services.cart;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidCartException;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.CartItem;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.CartRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Changes a line's quantity. The product need not be published anymore, so a buyer can still lower a
 * line whose product was suspended after it was added (cart-services.md).
 */
@Service
@RequiredArgsConstructor
public class UpdateCartItemQuantityService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Cart updateCartItemQuantity(User requestingUser, CartItem item) {
        User buyer = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(buyer, SystemRole.BUYER);
        if (item == null || item.getVariant() == null || item.getQuantity() == null) {
            throw new InvalidCartException("The line must carry its variant and quantity.");
        }
        Cart cart = cartRepositoryPort.findActiveByBuyer(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Active cart"));

        int previousQuantity = cart.updateItemQuantity(item.getVariant(), item.getQuantity());
        cartRepositoryPort.update(cart);

        Map<String, Object> details = new HashMap<>();
        details.put("sku", cart.getCartItems().stream()
                .filter(line -> line.getVariant().getVariantId().equals(item.getVariant().getVariantId()))
                .map(line -> line.getVariant().getSku()).findFirst().orElse(null));
        details.put("previousQuantity", previousQuantity);
        details.put("newQuantity", item.getQuantity());
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.CART_ITEM_UPDATE, buyer,
                        AffectedEntityType.CART, cart.getIdentifier()),
                details);
        return cart;
    }
}
