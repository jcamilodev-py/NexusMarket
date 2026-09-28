package aplication.nexusMarket.domain.services.order;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidOrderException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.OrderItem;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import aplication.nexusMarket.domain.ports.out.CartRepositoryPort;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.inventory.ReserveInventoryService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Converts the buyer's active cart into an order pending payment, reserving every physical line and
 * opening a new empty cart (Seccion 6.1 step 5).
 *
 * <p>If any line cannot be reserved the exception propagates and the use-case transaction rolls back
 * the order and every reservation already made (order-services.md - Place Order).
 */
@Service
@RequiredArgsConstructor
public class PlaceOrderService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final CartRepositoryPort cartRepositoryPort;
    private final BuyerRepositoryPort buyerRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ReserveInventoryService reserveInventoryService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Order placeOrder(User requestingUser, Order order) {
        User requester = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(requester, SystemRole.BUYER);
        if (!(requester instanceof Buyer buyer)) {
            throw new UnauthorizedOperationException("Only a buyer places orders.");
        }
        if (!buyer.canPlaceOrders()) {
            throw new InvalidOrderException("A commercially restricted buyer cannot place orders.");
        }
        Cart confirmedCart = cartRepositoryPort.findActiveByBuyer(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Active cart"));

        Order placedOrder = orderRepositoryPort.save(
                Order.placeFrom(confirmedCart, order == null ? null : order.getShippingAddress()));
        for (OrderItem item : placedOrder.getOrderItems()) {
            if (item.getVariant().getProduct().requiresInventory()) {
                reserveInventoryService.execute(buyer, item);
            }
        }
        orderRepositoryPort.update(placedOrder);

        Cart newCart = cartRepositoryPort.save(Cart.openFor(buyer));
        buyer.assignActiveCart(newCart);
        buyerRepositoryPort.update(buyer);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.CART_CONFIRMATION, buyer,
                        AffectedEntityType.CART, confirmedCart.getIdentifier()),
                Map.of("orderId", placedOrder.getIdentifier()));
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.ORDER_PLACEMENT, buyer,
                        AffectedEntityType.ORDER, placedOrder.getIdentifier()),
                Map.of("totalAmount", placedOrder.getTotalAmount(), "currency", placedOrder.getCurrency().getCode(),
                        "itemCount", placedOrder.getOrderItems().size()));
        return placedOrder;
    }
}
