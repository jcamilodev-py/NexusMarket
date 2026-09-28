package aplication.nexusMarket.domain.services.buyer;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Replaces a buyer's delivery addresses. Existing orders keep the address copied at checkout.
 *
 * <p>The audit details record what changed, not the addresses themselves, which already live in the
 * profile and in each order (buyer-services.md - Update Buyer Addresses).
 */
@Service
@RequiredArgsConstructor
public class UpdateBuyerAddressesService {

    private final BuyerRepositoryPort buyerRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Buyer updateBuyerAddresses(User requestingUser, Buyer buyer) {
        User updatingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(updatingUser, SystemRole.BUYER);
        if (buyer == null || buyer.getUserId() == null) {
            throw new EntityNotFoundException("Buyer");
        }
        Buyer storedBuyer = buyerRepositoryPort.findById(buyer)
                .orElseThrow(() -> new EntityNotFoundException("Buyer"));
        validateBuyerOwnershipService.execute(updatingUser, storedBuyer);

        String previousPrimaryAddress = storedBuyer.getPrimaryAddress();
        storedBuyer.updateAddresses(buyer.getPrimaryAddress(), buyer.getAdditionalAddresses());
        buyerRepositoryPort.update(storedBuyer);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.BUYER_PROFILE_UPDATE, updatingUser,
                        AffectedEntityType.USER, storedBuyer.getUserId()),
                Map.of("primaryAddressChanged", !Objects.equals(previousPrimaryAddress, storedBuyer.getPrimaryAddress()),
                        "additionalAddressCount", storedBuyer.getAdditionalAddresses().size()));
        return storedBuyer;
    }
}
