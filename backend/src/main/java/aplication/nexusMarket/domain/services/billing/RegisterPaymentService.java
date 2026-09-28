package aplication.nexusMarket.domain.services.billing;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Payment;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.ports.out.PaymentGatewayPort;
import aplication.nexusMarket.domain.ports.out.PaymentRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.services.order.ConfirmOrderPaymentService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.PaymentStatus;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Registers a payment attempt for the order's full amount and resolves it through the external
 * financial party (inferred). The attempt is stored before validation, so an interrupted attempt
 * still leaves a trace; only APPROVED moves the order to PAID (Seccion 6.1 step 6).
 */
@Service
@RequiredArgsConstructor
public class RegisterPaymentService {

    private static final Map<PaymentStatus, OperationType> OUTCOME_OPERATIONS = Map.of(
            PaymentStatus.APPROVED, OperationType.PAYMENT_APPROVAL,
            PaymentStatus.REJECTED, OperationType.PAYMENT_REJECTION,
            PaymentStatus.FAILED, OperationType.PAYMENT_FAILURE);

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;
    private final ConfirmOrderPaymentService confirmOrderPaymentService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public Payment registerPayment(User requestingUser, Payment payment) {
        User buyer = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(buyer, SystemRole.BUYER);
        if (payment == null || payment.getOrder() == null || payment.getOrder().getIdentifier() == null) {
            throw new EntityNotFoundException("Order");
        }
        Order order = orderRepositoryPort.findById(payment.getOrder())
                .orElseThrow(() -> new EntityNotFoundException("Order"));
        validateBuyerOwnershipService.execute(buyer, order.getBuyer());

        Payment attempt = paymentRepositoryPort.save(Payment.attemptFor(order));
        order.registerPaymentAttempt(attempt);
        registerOperationAndAuditService.execute(
                Operation.register(OperationType.PAYMENT_REGISTRATION, buyer,
                        AffectedEntityType.PAYMENT, attempt.getIdentifier()),
                Map.of("orderId", order.getIdentifier(), "amount", attempt.getAmount(),
                        "currency", attempt.getCurrency().getCode()));

        attempt.resolve(paymentGatewayPort.validate(attempt));
        paymentRepositoryPort.update(attempt);
        registerOperationAndAuditService.execute(
                Operation.register(OUTCOME_OPERATIONS.get(attempt.getPaymentStatus()), buyer,
                        AffectedEntityType.PAYMENT, attempt.getIdentifier()),
                Map.of("orderId", order.getIdentifier()));

        if (attempt.isApproved()) {
            confirmOrderPaymentService.execute(buyer, order);
        }
        return attempt;
    }
}
