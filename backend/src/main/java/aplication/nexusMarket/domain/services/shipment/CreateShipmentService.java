package aplication.nexusMarket.domain.services.shipment;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.LogisticsOperator;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Shipment;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.models.Warehouse;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ShipmentRepositoryPort;
import aplication.nexusMarket.domain.ports.out.WarehouseRepositoryPort;
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
 * Packs every unshipped physical line reserved from the chosen warehouse. The Domain, not the client,
 * picks the lines, so none is forgotten, duplicated or shipped from a warehouse without its stock
 * (shipment-services.md - Create Shipment).
 */
@Service
@RequiredArgsConstructor
public class CreateShipmentService {

    private final ShipmentRepositoryPort shipmentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Shipment createShipment(User requestingUser, Shipment shipment) {
        User requester = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(requester, SystemRole.LOGISTICS_OPERATOR);
        if (!(requester instanceof LogisticsOperator operator)) {
            throw new UnauthorizedOperationException("Only a logistics operator creates shipments.");
        }
        if (shipment == null || shipment.getOrder() == null || shipment.getOriginWarehouse() == null) {
            throw new EntityNotFoundException("Order or warehouse");
        }
        Order order = orderRepositoryPort.findById(shipment.getOrder())
                .orElseThrow(() -> new EntityNotFoundException("Order"));
        Warehouse warehouse = warehouseRepositoryPort.findById(shipment.getOriginWarehouse())
                .orElseThrow(() -> new EntityNotFoundException("Warehouse"));

        Shipment newShipment = Shipment.prepare(order, warehouse, order.unshippedPhysicalItemsFrom(warehouse), operator);
        Shipment createdShipment = shipmentRepositoryPort.save(newShipment);
        order.addShipment(createdShipment);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.SHIPMENT_CREATION, operator,
                        AffectedEntityType.SHIPMENT, createdShipment.getIdentifier()),
                Map.of("orderId", order.getIdentifier(), "warehouseId", warehouse.getIdentifier(),
                        "itemCount", createdShipment.getItems().size()));
        return createdShipment;
    }
}
