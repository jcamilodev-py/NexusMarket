package aplication.nexusMarket.domain.services.reporting;

import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Current stock per variant and warehouse. It reflects the present rather than a period: stock is a
 * state, and its history is Consult Inventory Movements.
 */
@Service
@RequiredArgsConstructor
public class ConsultInventoryReportService {

    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;

    public List<Inventory> consultInventoryReport(User requestingUser) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.SUPERVISOR, SystemRole.ADMINISTRATOR);
        return inventoryRepositoryPort.findAll();
    }
}
