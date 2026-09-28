package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataInventoryMovementRepository;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import aplication.nexusMarket.domain.ports.out.InventoryMovementRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Append-only: movements are immutable, so there is no update. */
@Repository
@RequiredArgsConstructor
public class InventoryMovementJpaAdapter implements InventoryMovementRepositoryPort {

    private final SpringDataInventoryMovementRepository movementRepository;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public InventoryMovement save(InventoryMovement movement) {
        movement.setIdentifier(Identifiers.orNew(movement.getIdentifier()));
        movementRepository.save(InventoryJpaMapper.toEntity(movement));
        return movement;
    }

    @Override
    public List<InventoryMovement> findByInventory(Inventory inventory) {
        return movementRepository.findByInventoryIdOrderByMovementDateAsc(inventory.getIdentifier()).stream()
                .map(entity -> InventoryJpaMapper.toDomain(entity, inventory,
                        referenceResolver.user(entity.getPerformedById())))
                .toList();
    }
}
