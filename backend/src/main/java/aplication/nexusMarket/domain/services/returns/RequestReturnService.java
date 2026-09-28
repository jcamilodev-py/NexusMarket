package aplication.nexusMarket.domain.services.returns;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Operation;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ReturnRequestRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.services.operation.RegisterOperationAndAuditService;
import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The buyer of a delivered order asks to return some of its lines (OBJ-11). Earlier non-rejected
 * requests count against each line, so nothing is returned twice (return-refund-services.md).
 */
@Service
@RequiredArgsConstructor
public class RequestReturnService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;
    private final RegisterOperationAndAuditService registerOperationAndAuditService;

    public ReturnRequest requestReturn(User requestingUser, ReturnRequest request) {
        User buyer = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(buyer, SystemRole.BUYER);
        if (request == null || request.getOrder() == null || request.getOrder().getIdentifier() == null) {
            throw new EntityNotFoundException("Order");
        }
        Order order = orderRepositoryPort.findById(request.getOrder())
                .orElseThrow(() -> new EntityNotFoundException("Order"));
        validateBuyerOwnershipService.execute(buyer, order.getBuyer());

        ReturnRequest newRequest = ReturnRequest.request(order, request.getReturnItems(), request.getReason(),
                returnRequestRepositoryPort.findByOrder(order));
        ReturnRequest savedRequest = returnRequestRepositoryPort.save(newRequest);

        registerOperationAndAuditService.execute(
                Operation.register(OperationType.RETURN_REQUEST_CREATION, buyer,
                        AffectedEntityType.RETURN_REQUEST, savedRequest.getIdentifier()),
                Map.of("orderId", order.getIdentifier(), "itemCount", savedRequest.getReturnItems().size(),
                        "refundableAmount", savedRequest.totalRefundableAmount()));
        return savedRequest;
    }
}
