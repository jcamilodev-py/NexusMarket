package aplication.nexusMarket.domain.services.returns;

import aplication.nexusMarket.domain.exceptions.EntityNotFoundException;
import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.RefundRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ReturnRequestRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateBuyerOwnershipService;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** A buyer consults their own returns and refunds; administrators and supervisors any. */
@Service
@RequiredArgsConstructor
public class ConsultReturnsAndRefundsService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final RefundRepositoryPort refundRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;
    private final ValidateBuyerOwnershipService validateBuyerOwnershipService;

    public List<ReturnRequest> consultReturns(User requestingUser) {
        User consultingUser = authorize(requestingUser);
        return consultingUser.hasRole(SystemRole.BUYER)
                ? returnRequestRepositoryPort.findByBuyer(consultingUser)
                : returnRequestRepositoryPort.findAll();
    }

    public Refund consultRefund(User requestingUser, ReturnRequest request) {
        User consultingUser = authorize(requestingUser);
        if (request == null || request.getIdentifier() == null) {
            throw new EntityNotFoundException("Return request");
        }
        ReturnRequest storedRequest = returnRequestRepositoryPort.findById(request)
                .orElseThrow(() -> new EntityNotFoundException("Return request"));
        if (consultingUser.hasRole(SystemRole.BUYER)) {
            validateBuyerOwnershipService.execute(consultingUser, storedRequest.getRequestedBy());
        }
        return refundRepositoryPort.findByReturnRequest(storedRequest)
                .orElseThrow(() -> new EntityNotFoundException("Refund"));
    }

    private User authorize(User requestingUser) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser,
                SystemRole.BUYER, SystemRole.ADMINISTRATOR, SystemRole.SUPERVISOR);
        return consultingUser;
    }
}
