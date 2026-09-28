package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Warehouse;
import java.util.Optional;

/** findById returns the concrete specialization, with the owner of a seller warehouse. */
public interface WarehouseRepositoryPort {

    <T extends Warehouse> T save(T warehouse);

    Optional<Warehouse> findById(Warehouse warehouse);

    void update(Warehouse warehouse);
}
