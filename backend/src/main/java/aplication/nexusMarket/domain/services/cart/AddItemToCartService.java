package aplication.nexusMarket.domain.services.cart;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidCartException;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.CartItem;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.CartRepositoryPort;
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
 * Adds a published variant to the requester's own active cart. Stock is not checked: the cart
 * reserves nothing, and availability is decided at checkout (cart-services.md - Add Item to Cart).
 */
@Service
@RequiredArgsConstructor
public class AddItemToCartService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Cart addItemToCart(User requestingUser, CartItem item) {
        User buyer = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(buyer, SystemRole.BUYER);
        if (item == null || item.getVariant() == null || item.getQuantity() == null) {
            throw new InvalidCartException("The line must carry its variant and quantity.");
        }
        Cart cart = cartRepositoryPort.findActiveByBuyer(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Active cart"));
        ProductVariant variant = productRepositoryPort.findVariantById(item.getVariant())
                .orElseThrow(() -> new EntityNotFoundException("Product variant"));

        CartItem line = cart.addItem(variant, item.getQuantity());
        cartRepositoryPort.update(cart);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.CART_ITEM_ADDITION, buyer,
                        AffectedEntityType.CART, cart.getIdentifier()),
                Map.of("sku", variant.getSku(), "quantityAdded", item.getQuantity(), "lineQuantity", line.getQuantity()));
        return cart;
    }
}
