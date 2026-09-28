package aplication.nexusMarket.domain.services.shipment;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Shipment;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ShipmentRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Shipments of an order, for its buyer and for logistics, administrative and supervision staff. */
@Service
@RequiredArgsConstructor
public class ConsultShipmentsService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public List<Shipment> consultShipments(User requestingUser, Order order) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.BUYER,
                SystemRole.LOGISTICS_OPERATOR, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (order == null || order.getIdentifier() == null) {
            throw new EntityNotFoundException("Order");
        }
        Order storedOrder = orderRepositoryPort.findById(order)
                .orElseThrow(() -> new EntityNotFoundException("Order"));
        if (consultingUser.hasRole(SystemRole.BUYER)) {
            validateBuyerOwnershipService.execute(consultingUser, storedOrder.getBuyer());
        }
        return shipmentRepositoryPort.findByOrder(storedOrder);
    }
}
