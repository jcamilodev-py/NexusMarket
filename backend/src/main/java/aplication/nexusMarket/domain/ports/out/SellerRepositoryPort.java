package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Seller;
import java.util.Optional;

/** findById loads the seller's warehouses but not their products. */
public interface SellerRepositoryPort {

    Seller save(Seller seller);

    Optional<Seller> findById(Seller seller);

    void update(Seller seller);
}
