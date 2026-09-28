package aplication.nexusMarket.domain.services.order;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * A buyer sees their own orders and a seller the orders holding their products - the whole order, as
 * a single commitment; staff roles see any (Matriz de Responsabilidades; RG-03).
 */
@Service
@RequiredArgsConstructor
public class ConsultOrderService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public Order consultOrder(User requestingUser, Order order) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.BUYER, SystemRole.SELLER,
                SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (order == null || order.getIdentifier() == null) {
            throw new EntityNotFoundException("Order");
        }
        Order storedOrder = orderRepositoryPort.findById(order)
                .orElseThrow(() -> new EntityNotFoundException("Order"));

        if (consultingUser.hasRole(SystemRole.BUYER)) {
            validateBuyerOwnershipService.execute(consultingUser, storedOrder.getBuyer());
        }
        if (consultingUser.hasRole(SystemRole.SELLER) && !storedOrder.containsProductsOf(consultingUser)) {
            throw new UnauthorizedOperationException("The order contains no products of this seller.");
        }
        return storedOrder;
    }
}
