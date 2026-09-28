package aplication.nexusMarket.domain.services.authorization;

import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Accepts a seller only over what they own: the caller passes the owner it already holds, such as
 * product.getSeller() or sellerWarehouse.getOwner() (Seccion 5; RG-03).
 */
@Service
@RequiredArgsConstructor
public class ValidateSellerOwnershipService {

    public void execute(User requestingUser, Seller owner) {
        if (requestingUser == null || owner == null
                || !requestingUser.hasRole(SystemRole.SELLER)
                || !Objects.equals(requestingUser.getUserId(), owner.getUserId())) {
            throw new UnauthorizedOperationException("The seller does not own this information.");
        }
    }
}
