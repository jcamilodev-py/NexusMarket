package aplication.nexusMarket.domain.services.buyer;

import aplication.nexusMarket.domain.exceptions.InvalidCredentialsException;
import aplication.nexusMarket.domain.exceptions.InvalidUserException;
import aplication.nexusMarket.domain.exceptions.UserAlreadyExistsException;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Cart;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import aplication.nexusMarket.domain.ports.out.CartRepositoryPort;
import aplication.nexusMarket.domain.ports.out.PasswordServicePort;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.BuyerCommercialStatus;
import aplication.nexusMarket.domain.valueobjects.Credentials;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import aplication.nexusMarket.domain.valueobjects.UserStatus;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Registers a buyer who signs up by themself (Seccion 3.1), with an empty active cart.
 *
 * <p>The only service besides Login that runs without an authenticated user, so the new buyer is the
 * performer of their own registration (buyer-services.md - Register Buyer).
 */
@Service
@RequiredArgsConstructor
public class RegisterBuyerService {

    private final BuyerRepositoryPort buyerRepositoryPort;
    private final CartRepositoryPort cartRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordServicePort passwordServicePort;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Buyer registerBuyer(Buyer newBuyer, Credentials credentials) {
        if (newBuyer == null) {
            throw new InvalidUserException("The buyer to register must be provided.");
        }
        newBuyer.validateIdentity();
        newBuyer.updateAddresses(newBuyer.getPrimaryAddress(), newBuyer.getAdditionalAddresses());
        if (credentials == null || !credentials.email().equals(newBuyer.getEmail())) {
            throw new InvalidCredentialsException();
        }
        if (userRepositoryPort.existsByEmail(newBuyer)) {
            throw new UserAlreadyExistsException("A user with this email is already registered.");
        }
        if (userRepositoryPort.existsByIdentificationNumber(newBuyer)) {
            throw new UserAlreadyExistsException("A user with this identification number is already registered.");
        }

        newBuyer.assignPasswordHash(passwordServicePort.encode(credentials));
        newBuyer.setRole(SystemRole.BUYER);
        newBuyer.setStatus(UserStatus.ACTIVE);
        newBuyer.setCommercialStatus(BuyerCommercialStatus.ENABLED);
        Buyer registeredBuyer = buyerRepositoryPort.save(newBuyer);

        Cart activeCart = cartRepositoryPort.save(Cart.openFor(registeredBuyer));
        registeredBuyer.assignActiveCart(activeCart);
        buyerRepositoryPort.update(registeredBuyer);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.BUYER_REGISTRATION, registeredBuyer,
                        AffectedEntityType.USER, registeredBuyer.getUserId()),
                Map.of());
        return registeredBuyer;
    }
}
