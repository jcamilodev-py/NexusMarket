package aplication.nexusMarket.domain.services.cart;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.CartRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Returns the requester's active cart; Cart.calculateTotal values it at current catalog prices. */
@Service
@RequiredArgsConstructor
public class ConsultCartService {

    private final CartRepositoryPort cartRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;

    public Cart consultCart(User requestingUser) {
        User buyer = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(buyer, SystemRole.BUYER);
        return cartRepositoryPort.findActiveByBuyer(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Active cart"));
    }
}
