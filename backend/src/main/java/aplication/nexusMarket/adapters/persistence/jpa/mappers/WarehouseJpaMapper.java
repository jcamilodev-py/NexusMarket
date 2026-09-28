package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import aplication.nexusMarket.domain.models.MarketplaceWarehouse;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.SellerWarehouse;
import aplication.nexusMarket.domain.models.Warehouse;

public final class WarehouseJpaMapper {

    private WarehouseJpaMapper() {
    }

    public static WarehouseJpaEntity toEntity(Warehouse domain) {
        WarehouseJpaEntity entity = new WarehouseJpaEntity();
        entity.setWarehouseId(domain.getIdentifier());
        entity.setAddress(domain.getAddress());
        if (domain instanceof SellerWarehouse sellerWarehouse) {
            entity.setWarehouseType(WarehouseJpaEntity.SELLER);
            entity.setOwnerId(sellerWarehouse.getOwner() == null ? null : sellerWarehouse.getOwner().getUserId());
        } else {
            entity.setWarehouseType(WarehouseJpaEntity.MARKETPLACE);
        }
        return entity;
    }

    /** The owner is resolved by the caller; it is ignored for a marketplace warehouse. */
    public static Warehouse toDomain(WarehouseJpaEntity entity, Seller owner) {
        Warehouse domain;
        if (WarehouseJpaEntity.SELLER.equals(entity.getWarehouseType())) {
            SellerWarehouse sellerWarehouse = new SellerWarehouse();
            sellerWarehouse.setOwner(owner);
            domain = sellerWarehouse;
        } else {
            domain = new MarketplaceWarehouse();
        }
        domain.setIdentifier(entity.getWarehouseId());
        domain.setAddress(entity.getAddress());
        return domain;
    }
}
