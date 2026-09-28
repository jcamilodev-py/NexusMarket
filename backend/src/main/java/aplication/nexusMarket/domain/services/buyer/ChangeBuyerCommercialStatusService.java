package aplication.nexusMarket.domain.services.buyer;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.BuyerCommercialStatus;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Enables or restricts a buyer's ability to place orders; their UserStatus is never touched.
 *
 * <p>Executed by an Administrator, inferred from DOMINIO 2 and Seccion 5 (buyer-services.md - Change
 * Buyer Commercial Status).
 */
@Service
@RequiredArgsConstructor
public class ChangeBuyerCommercialStatusService {

    private final BuyerRepositoryPort buyerRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Buyer changeBuyerCommercialStatus(User requestingUser, Buyer buyer) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (buyer == null || buyer.getUserId() == null) {
            throw new EntityNotFoundException("Buyer");
        }
        Buyer storedBuyer = buyerRepositoryPort.findById(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Buyer"));

        BuyerCommercialStatus previousStatus = storedBuyer.getCommercialStatus();
        storedBuyer.changeCommercialStatus(buyer.getCommercialStatus());
        buyerRepositoryPort.update(storedBuyer);

        Map<String, Object> details = new HashMap<>();
        details.put("previousStatus", previousStatus == null ? null : previousStatus.getCode());
        details.put("newStatus", storedBuyer.getCommercialStatus().getCode());
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.BUYER_COMMERCIAL_STATUS_CHANGE, administrator,
                        AffectedEntityType.USER, storedBuyer.getUserId()),
                details);
        return storedBuyer;
    }
}
