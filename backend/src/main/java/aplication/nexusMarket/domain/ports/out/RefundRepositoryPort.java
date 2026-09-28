package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.ReturnRequest;
import java.util.Optional;

public interface RefundRepositoryPort {

    Refund save(Refund refund);

    Optional<Refund> findById(Refund refund);

    Optional<Refund> findByReturnRequest(ReturnRequest request);

    void update(Refund refund);
}
