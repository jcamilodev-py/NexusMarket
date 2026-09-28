package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import aplication.nexusMarket.domain.models.LogisticsOperator;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Shipment;
import aplication.nexusMarket.domain.models.Warehouse;
import aplication.nexusMarket.domain.valueobjects.ShipmentStatus;
import java.util.ArrayList;

/**
 * Stored lines are variant identifiers; reading matches them to the order's own lines, so a shipment
 * never carries a copy of an order line.
 */
public final class ShipmentJpaMapper {

    private ShipmentJpaMapper() {
    }

    public static ShipmentJpaEntity toEntity(Shipment domain) {
        ShipmentJpaEntity entity = new ShipmentJpaEntity();
        entity.setShipmentId(domain.getIdentifier());
        entity.setOrderId(domain.getOrder().getIdentifier());
        entity.setOriginWarehouseId(domain.getOriginWarehouse().getIdentifier());
        entity.setLogisticsOperatorId(domain.getLogisticsOperator().getUserId());
        entity.setShipmentStatus(CatalogCodes.code(domain.getShipmentStatus()));
        entity.setDispatchDate(domain.getDispatchDate());
        entity.setDeliveryDate(domain.getDeliveryDate());
        entity.setVariantIds(new ArrayList<>(domain.getItems().stream()
                .map(item -> item.getVariant().getVariantId())
                .toList()));
        return entity;
    }

    public static Shipment toDomain(ShipmentJpaEntity entity, Order order, Warehouse originWarehouse,
                                    LogisticsOperator logisticsOperator) {
        Shipment domain = new Shipment();
        domain.setIdentifier(entity.getShipmentId());
        domain.setOrder(order);
        domain.setOriginWarehouse(originWarehouse);
        domain.setLogisticsOperator(logisticsOperator);
        domain.setShipmentStatus(CatalogCodes.fromCode(ShipmentStatus.class, entity.getShipmentStatus()));
        domain.setDispatchDate(entity.getDispatchDate());
        domain.setDeliveryDate(entity.getDeliveryDate());
        domain.setItems(new ArrayList<>(order.getOrderItems().stream()
                .filter(item -> entity.getVariantIds().contains(item.getVariant().getVariantId()))
                .toList()));
        return domain;
    }
}
