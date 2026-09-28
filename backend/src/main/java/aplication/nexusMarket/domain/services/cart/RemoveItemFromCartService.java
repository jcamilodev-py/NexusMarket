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
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Removes a line from the requester's own active cart (OBJ-07). */
@Service
@RequiredArgsConstructor
public class RemoveItemFromCartService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Cart removeItemFromCart(User requestingUser, CartItem item) {
        User buyer = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(buyer, SystemRole.BUYER);
        if (item == null || item.getVariant() == null) {
            throw new InvalidCartException("The line to remove must carry its variant.");
        }
        Cart cart = cartRepositoryPort.findActiveByBuyer(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Active cart"));

        CartItem removedLine = cart.removeItem(item.getVariant());
        cartRepositoryPort.update(cart);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.CART_ITEM_REMOVAL, buyer,
                        AffectedEntityType.CART, cart.getIdentifier()),
                Map.of("sku", removedLine.getVariant().getSku(), "removedQuantity", removedLine.getQuantity()));
        return cart;
    }
}
