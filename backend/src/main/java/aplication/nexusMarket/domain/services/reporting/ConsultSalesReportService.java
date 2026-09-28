package aplication.nexusMarket.domain.services.reporting;

import aplication.nexusMarket.domain.exceptions.InvalidReportException;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.services.authorization.ValidateRolePermissionService;
import aplication.nexusMarket.domain.services.authorization.ValidateUserStatusService;
import aplication.nexusMarket.domain.valueobjects.ReportPeriod;
import aplication.nexusMarket.domain.valueobjects.SalesSummary;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Orders of a period by status and currency, for supervision (inferred report of OBJ-12). */
@Service
@RequiredArgsConstructor
public class ConsultSalesReportService {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ValidateUserStatusService validateUserStatusService;
    private final ValidateRolePermissionService validateRolePermissionService;

    public List<SalesSummary> consultSalesReport(User requestingUser, ReportPeriod period) {
        User consultingUser = validateUserStatusService.execute(requestingUser);
        validateRolePermissionService.execute(consultingUser, SystemRole.SUPERVISOR, SystemRole.ADMINISTRATOR);
        if (period == null) {
            throw new InvalidReportException("The report period must be provided.");
        }
        return SalesSummary.summarize(orderRepositoryPort.findByPeriod(period));
    }
}
