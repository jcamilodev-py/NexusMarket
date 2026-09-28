package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.ReportPeriod;
import java.util.List;
import java.util.Optional;

/** Lookups return each line with its order line, variant, product and source inventory. */
public interface ReturnRequestRepositoryPort {

    ReturnRequest save(ReturnRequest request);

    Optional<ReturnRequest> findById(ReturnRequest request);

    List<ReturnRequest> findByOrder(Order order);

    List<ReturnRequest> findByBuyer(User buyer);

    List<ReturnRequest> findAll();

    List<ReturnRequest> findByPeriod(ReportPeriod period);

    void update(ReturnRequest request);
}
