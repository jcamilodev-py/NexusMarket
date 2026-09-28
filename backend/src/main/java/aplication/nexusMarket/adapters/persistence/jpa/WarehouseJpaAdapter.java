package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataWarehouseRepository;
import aplication.nexusMarket.domain.models.Warehouse;
import aplication.nexusMarket.domain.ports.out.WarehouseRepositoryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WarehouseJpaAdapter implements WarehouseRepositoryPort {

    private final SpringDataWarehouseRepository warehouseRepository;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public <T extends Warehouse> T save(T warehouse) {
        warehouse.setIdentifier(Identifiers.orNew(warehouse.getIdentifier()));
        warehouseRepository.save(WarehouseJpaMapper.toEntity(warehouse));
        return warehouse;
    }

    @Override
    public Optional<Warehouse> findById(Warehouse warehouse) {
        if (warehouse == null || warehouse.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(referenceResolver.warehouse(warehouse.getIdentifier()));
    }

    @Override
    public void update(Warehouse warehouse) {
        warehouseRepository.save(WarehouseJpaMapper.toEntity(warehouse));
    }
}
