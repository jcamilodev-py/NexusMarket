package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Buyer;
import java.util.Optional;

public interface BuyerRepositoryPort {

    Buyer save(Buyer buyer);

    Optional<Buyer> findById(Buyer buyer);

    void update(Buyer buyer);
}
