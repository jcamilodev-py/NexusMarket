package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.OrderItemEmbeddable;
import aplication.nexusMarket.adapters.persistence.jpa.entities.OrderJpaEntity;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.OrderItem;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.OrderStatus;
import java.util.ArrayList;
import java.util.function.Function;

/**
 * The order and its frozen lines. Payments, invoice and shipments are separate records the adapter
 * attaches afterwards.
 */
public final class OrderJpaMapper {

    private OrderJpaMapper() {
    }

    public static OrderJpaEntity toEntity(Order domain) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setOrderId(domain.getIdentifier());
        entity.setBuyerId(domain.getBuyer().getUserId());
        entity.setShippingAddress(domain.getShippingAddress());
        entity.setOrderStatus(CatalogCodes.code(domain.getOrderStatus()));
        entity.setCreationDate(domain.getCreationDate());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setCurrency(CatalogCodes.code(domain.getCurrency()));
        entity.setItems(new ArrayList<>(domain.getOrderItems().stream().map(OrderJpaMapper::toEmbeddable).toList()));
        return entity;
    }

    public static Order toDomain(OrderJpaEntity entity, Buyer buyer,
                                 Function<String, ProductVariant> variants, Function<String, Inventory> inventories) {
        Order domain = new Order();
        domain.setIdentifier(entity.getOrderId());
        domain.setBuyer(buyer);
        domain.setShippingAddress(entity.getShippingAddress());
        domain.setOrderStatus(CatalogCodes.fromCode(OrderStatus.class, entity.getOrderStatus()));
        domain.setCreationDate(entity.getCreationDate());
        domain.setTotalAmount(entity.getTotalAmount());
        domain.setCurrency(CatalogCodes.fromCode(Currency.class, entity.getCurrency()));
        for (OrderItemEmbeddable line : entity.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(domain);
            item.setVariant(variants.apply(line.getVariantId()));
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(line.getUnitPrice());
            item.setSubtotal(line.getSubtotal());
            item.setSourceInventory(line.getSourceInventoryId() == null ? null : inventories.apply(line.getSourceInventoryId()));
            domain.getOrderItems().add(item);
        }
        return domain;
    }

    private static OrderItemEmbeddable toEmbeddable(OrderItem item) {
        OrderItemEmbeddable line = new OrderItemEmbeddable();
        line.setVariantId(item.getVariant().getVariantId());
        line.setQuantity(item.getQuantity());
        line.setUnitPrice(item.getUnitPrice());
        line.setSubtotal(item.getSubtotal());
        line.setSourceInventoryId(item.getSourceInventory() == null ? null : item.getSourceInventory().getIdentifier());
        return line;
    }
}
