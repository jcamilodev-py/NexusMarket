package aplication.nexusMarket.domain.services.shipment;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Shipment;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.ShipmentRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.services.order.MarkOrderDeliveredService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Confirms a delivery; the last delivered shipment closes the order (Seccion 6.1 step 8). */
@Service
@RequiredArgsConstructor
public class ConfirmShipmentDeliveryService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final MarkOrderDeliveredService markOrderDeliveredService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Shipment confirmShipmentDelivery(User requestingUser, Shipment shipment) {
        User operator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(operator, SystemRole.LOGISTICS_OPERATOR);
        if (shipment == null || shipment.getIdentifier() == null) {
            throw new EntityNotFoundException("Shipment");
        }
        Shipment storedShipment = shipmentRepositoryPort.findById(shipment)
                .orElseThrow(() -> new EntityNotFoundException("Shipment"));

        storedShipment.confirmDelivery();
        shipmentRepositoryPort.update(storedShipment);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.SHIPMENT_DELIVERY, operator,
                        AffectedEntityType.SHIPMENT, storedShipment.getIdentifier()),
                Map.of("orderId", storedShipment.getOrder().getIdentifier()));
        markOrderDeliveredService.execute(operator, storedShipment.getOrder());
        return storedShipment;
    }
}
