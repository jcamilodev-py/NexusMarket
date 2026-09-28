package aplication.nexusMarket.domain.services.shipment;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Shipment;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.ShipmentRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.inventory.RegisterSaleOutboundService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.services.order.MarkOrderDispatchedService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Dispatches a shipment: its stock leaves inventory and the first dispatch of an order moves it to
 * DISPATCHED. Any logistics operator may do it, since the specification assigns the physical operation
 * to the role, not to a person (shipment-services.md - Dispatch Shipment).
 */
@Service
@RequiredArgsConstructor
public class DispatchShipmentService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterSaleOutboundService registerSaleOutboundService;
    private final MarkOrderDispatchedService markOrderDispatchedService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Shipment dispatchShipment(User requestingUser, Shipment shipment) {
        User operator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(operator, SystemRole.LOGISTICS_OPERATOR);
        if (shipment == null || shipment.getIdentifier() == null) {
            throw new EntityNotFoundException("Shipment");
        }
        Shipment storedShipment = shipmentRepositoryPort.findById(shipment)
                .orElseThrow(() -> new EntityNotFoundException("Shipment"));

        storedShipment.dispatch();
        storedShipment.getItems().forEach(item -> registerSaleOutboundService.execute(operator, item));
        shipmentRepositoryPort.update(storedShipment);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.SHIPMENT_DISPATCH, operator,
                        AffectedEntityType.SHIPMENT, storedShipment.getIdentifier()),
                Map.of("orderId", storedShipment.getOrder().getIdentifier(),
                        "warehouseId", storedShipment.getOriginWarehouse().getIdentifier()));
        markOrderDispatchedService.execute(operator, storedShipment.getOrder());
        return storedShipment;
    }
}
