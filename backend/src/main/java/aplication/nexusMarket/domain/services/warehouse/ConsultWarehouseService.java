package aplication.nexusMarket.domain.services.warehouse;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.SellerWarehouse;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.models.Warehouse;
import aplication.nexusMarket.domain.ports.out.WarehouseRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Staff roles consult any warehouse; a seller only their own, so never a marketplace warehouse,
 * which has no seller owner (warehouse-services.md - Consult Warehouse).
 */
@Service
@RequiredArgsConstructor
public class ConsultWarehouseService {

    private final WarehouseRepositoryPort warehouseRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public Warehouse consultWarehouse(User requestingUser, Warehouse warehouse) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.ADMINISTRATOR,
                SystemRole.LOGISTICS_OPERATOR, SystemRole.SUPERVISOR, SystemRole.SELLER);
        if (warehouse == null || warehouse.getIdentifier() == null) {
            throw new EntityNotFoundException("Warehouse");
        }
        Warehouse storedWarehouse = warehouseRepositoryPort.findById(warehouse)
                .orElseThrow(() -> new EntityNotFoundException("Warehouse"));

        if (consultingUser.hasRole(SystemRole.SELLER)) {
            if (!(storedWarehouse instanceof SellerWarehouse sellerWarehouse)) {
                throw new UnauthorizedOperationException("A seller may only consult their own warehouses.");
            }
            validateSellerOwnershipService.execute(consultingUser, sellerWarehouse.getOwner());
        }
        return storedWarehouse;
    }
}
