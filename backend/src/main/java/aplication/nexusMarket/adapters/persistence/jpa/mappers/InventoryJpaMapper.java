package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.InventoryJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.InventoryMovement;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.models.Warehouse;
import aplication.nexusMarket.domain.valueobjects.InventoryMovementType;
import aplication.nexusMarket.domain.valueobjects.InventoryStatus;

/** Inventory records and their movements; variant, warehouse and performer are resolved by the caller. */
public final class InventoryJpaMapper {

    private InventoryJpaMapper() {
    }

    public static InventoryJpaEntity toEntity(Inventory domain) {
        InventoryJpaEntity entity = new InventoryJpaEntity();
        entity.setInventoryId(domain.getIdentifier());
        entity.setVariantId(domain.getVariant().getVariantId());
        entity.setWarehouseId(domain.getWarehouse().getIdentifier());
        entity.setAvailableQuantity(domain.getAvailableQuantity());
        entity.setReservedQuantity(domain.getReservedQuantity());
        entity.setInventoryStatus(CatalogCodes.code(domain.getInventoryStatus()));
        return entity;
    }

    public static Inventory toDomain(InventoryJpaEntity entity, ProductVariant variant, Warehouse warehouse) {
        Inventory domain = new Inventory();
        domain.setIdentifier(entity.getInventoryId());
        domain.setVariant(variant);
        domain.setWarehouse(warehouse);
        domain.setAvailableQuantity(entity.getAvailableQuantity());
        domain.setReservedQuantity(entity.getReservedQuantity());
        domain.setInventoryStatus(CatalogCodes.fromCode(InventoryStatus.class, entity.getInventoryStatus()));
        return domain;
    }

    public static InventoryMovementJpaEntity toEntity(InventoryMovement domain) {
        InventoryMovementJpaEntity entity = new InventoryMovementJpaEntity();
        entity.setMovementId(domain.getIdentifier());
        entity.setInventoryId(domain.getInventory().getIdentifier());
        entity.setMovementType(CatalogCodes.code(domain.getMovementType()));
        entity.setQuantity(domain.getQuantity());
        entity.setMovementDate(domain.getMovementDate());
        entity.setPerformedById(domain.getPerformedBy().getUserId());
        return entity;
    }

    public static InventoryMovement toDomain(InventoryMovementJpaEntity entity, Inventory inventory, User performedBy) {
        InventoryMovement domain = new InventoryMovement();
        domain.setIdentifier(entity.getMovementId());
        domain.setInventory(inventory);
        domain.setMovementType(CatalogCodes.fromCode(InventoryMovementType.class, entity.getMovementType()));
        domain.setQuantity(entity.getQuantity());
        domain.setMovementDate(entity.getMovementDate());
        domain.setPerformedBy(performedBy);
        return domain;
    }
}
