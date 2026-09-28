package aplication.nexusMarket.domain.services.reporting;

import aplication.nexusMarket.domain.exceptions.InvalidReportException;
import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.RefundRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ReturnRequestRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.ReportPeriod;
import aplication.nexusMarket.domain.valueobjects.ReturnSummary;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Return requests of a period by status and currency, with the money actually refunded (OBJ-11, OBJ-12). */
@Service
@RequiredArgsConstructor
public class ConsultReturnsAndRefundsReportService {

    private final ReturnRequestRepositoryPort returnRequestRepositoryPort;
    private final RefundRepositoryPort refundRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;

    public List<ReturnSummary> consultReturnsAndRefundsReport(User requestingUser, ReportPeriod period) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.SUPERVISOR, SystemRole.ADMINISTRATOR);
        if (period == null) {
            throw new InvalidReportException("The report period must be provided.");
        }
        List<ReturnRequest> requests = returnRequestRepositoryPort.findByPeriod(period);
        List<Refund> refunds = requests.stream()
                .map(refundRepositoryPort::findByReturnRequest)
                .flatMap(Optional::stream)
                .toList();
        return ReturnSummary.summarize(requests, refunds);
    }
}
