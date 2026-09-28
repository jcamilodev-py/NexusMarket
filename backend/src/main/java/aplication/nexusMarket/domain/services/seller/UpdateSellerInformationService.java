package aplication.nexusMarket.domain.services.seller;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.exceptions.InvalidSellerException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.SellerRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Maintains a seller's commercial identity, a task DOMINIO 3 gives the Administrator. An update that
 * changes nothing is rejected, since it would audit an event that did not happen.
 */
@Service
@RequiredArgsConstructor
public class UpdateSellerInformationService {

    private final SellerRepositoryPort sellerRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Seller updateSellerInformation(User requestingUser, Seller seller) {
        User administrator = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(administrator, SystemRole.ADMINISTRATOR);
        if (seller == null || seller.getUserId() == null) {
            throw new EntityNotFoundException("Seller");
        }
        Seller storedSeller = sellerRepositoryPort.findById(seller)
                .orElseThrow(() -> new EntityNotFoundException("Seller"));

        List<String> changedFields = storedSeller.updateCommercialIdentity(
                seller.getLegalBusinessName(), seller.getTaxId(), seller.getTradeName());
        if (changedFields.isEmpty()) {
            throw new InvalidSellerException("The update does not change the seller's information.");
        }
        sellerRepositoryPort.update(storedSeller);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.SELLER_UPDATE, administrator,
                        AffectedEntityType.SELLER, storedSeller.getUserId()),
                Map.of("changedFields", changedFields));
        return storedSeller;
    }
}
