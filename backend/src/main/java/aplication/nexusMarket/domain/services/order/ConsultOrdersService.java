package aplication.nexusMarket.domain.services.order;

import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Lists the orders each role may see, optionally filtered by status (order-services.md - Consult Orders). */
@Service
@RequiredArgsConstructor
public class ConsultOrdersService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;

    public List<Order> consultOrders(User requestingUser, Order criteria) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.BUYER, SystemRole.SELLER,
                SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        Order filter = criteria == null ? new Order() : criteria;

        List<Order> visibleOrders;
        if (consultingUser.hasRole(SystemRole.BUYER)) {
            visibleOrders = orderRepositoryPort.findByBuyer(consultingUser);
        } else if (consultingUser.hasRole(SystemRole.SELLER)) {
            visibleOrders = orderRepositoryPort.findBySeller(consultingUser);
        } else {
            return orderRepositoryPort.findByStatus(filter);
        }
        OrderStatus status = filter.getOrderStatus();
        return status == null ? visibleOrders
                : visibleOrders.stream().filter(order -> status.equals(order.getOrderStatus())).toList();
    }
}
