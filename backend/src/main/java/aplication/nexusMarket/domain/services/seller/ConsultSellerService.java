package aplication.nexusMarket.domain.services.seller;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.SellerRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateSellerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** A seller consults only their own information; administrators and supervisors any (RG-03). */
@Service
@RequiredArgsConstructor
public class ConsultSellerService {

    private final SellerRepositoryPort sellerRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateSellerOwnershipService validateSellerOwnershipService;

    public Seller consultSeller(User requestingUser, Seller seller) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser,
                SystemRole.SELLER, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        if (seller == null || seller.getUserId() == null) {
            throw new EntityNotFoundException("Seller");
        }
        Seller storedSeller = sellerRepositoryPort.findById(seller)
                .orElseThrow(() -> new EntityNotFoundException("Seller"));
        if (consultingUser.hasRole(SystemRole.SELLER)) {
            validateSellerOwnershipService.execute(consultingUser, storedSeller);
        }
        return storedSeller;
    }
}
