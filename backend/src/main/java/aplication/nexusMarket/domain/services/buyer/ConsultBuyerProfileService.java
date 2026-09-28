package aplication.nexusMarket.domain.services.buyer;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** A buyer consults only their own profile; administrators and supervisors any (DOMINIO 2; RG-03). */
@Service
@RequiredArgsConstructor
public class ConsultBuyerProfileService {

    private final BuyerRepositoryPort buyerRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public Buyer consultBuyerProfile(User requestingUser, Buyer buyer) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser,
                SystemRole.BUYER, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (buyer == null || buyer.getUserId() == null) {
            throw new EntityNotFoundException("Buyer");
        }
        Buyer storedBuyer = buyerRepositoryPort.findById(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Buyer"));
        if (consultingUser.hasRole(SystemRole.BUYER)) {
            validateBuyerOwnershipService.execute(consultingUser, storedBuyer);
        }
        return storedBuyer;
    }
}
