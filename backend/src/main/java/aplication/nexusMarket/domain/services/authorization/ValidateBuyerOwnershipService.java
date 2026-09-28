package aplication.nexusMarket.domain.services.authorization;

import aplication.nexusMarket.domain.exceptions.UnauthorizedOperationException;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Accepts a buyer only over what they own: the caller passes the owner it already holds, such as
 * order.getBuyer(), so one rule covers carts, orders, payments, invoices and returns (DOMINIO 2).
 */
@Service
@RequiredArgsConstructor
public class ValidateBuyerOwnershipService {

    public void execute(User requestingUser, Buyer owner) {
        if (requestingUser == null || owner == null
                || !requestingUser.hasRole(SystemRole.BUYER)
                || !Objects.equals(requestingUser.getUserId(), owner.getUserId())) {
            throw new UnauthorizedOperationException("The buyer does not own this information.");
        }
    }
}
