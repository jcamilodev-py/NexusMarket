package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.RefundJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.entities.ReturnItemEmbeddable;
import aplication.nexusMarket.adapters.persistence.jpa.entities.ReturnRequestJpaEntity;
import aplication.nexusMarket.domain.models.Administrator;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.ReturnItem;
import aplication.nexusMarket.domain.models.ReturnRequest;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.RefundStatus;
import aplication.nexusMarket.domain.valueobjects.ReturnStatus;
import java.util.ArrayList;

/** Returned lines are matched to the order's own lines by variant, keeping the frozen unit price reachable. */
public final class ReturnJpaMapper {

    private ReturnJpaMapper() {
    }

    public static ReturnRequestJpaEntity toEntity(ReturnRequest domain) {
        ReturnRequestJpaEntity entity = new ReturnRequestJpaEntity();
        entity.setReturnRequestId(domain.getIdentifier());
        entity.setOrderId(domain.getOrder().getIdentifier());
        entity.setRequestedById(domain.getRequestedBy().getUserId());
        entity.setReason(domain.getReason());
        entity.setRequestDate(domain.getRequestDate());
        entity.setReturnStatus(CatalogCodes.code(domain.getReturnStatus()));
        entity.setItems(new ArrayList<>(domain.getReturnItems().stream().map(ReturnJpaMapper::toEmbeddable).toList()));
        return entity;
    }

    public static ReturnRequest toDomain(ReturnRequestJpaEntity entity, Order order) {
        ReturnRequest domain = new ReturnRequest();
        domain.setIdentifier(entity.getReturnRequestId());
        domain.setOrder(order);
        domain.setRequestedBy(order.getBuyer());
        domain.setReason(entity.getReason());
        domain.setRequestDate(entity.getRequestDate());
        domain.setReturnStatus(CatalogCodes.fromCode(ReturnStatus.class, entity.getReturnStatus()));
        for (ReturnItemEmbeddable line : entity.getItems()) {
            ReturnItem item = new ReturnItem();
            item.setReturnRequest(domain);
            item.setOrderItem(order.getOrderItems().stream()
                    .filter(orderLine -> orderLine.getVariant().getVariantId().equals(line.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Returned line without order line: " + line.getVariantId())));
            item.setQuantity(line.getQuantity());
            item.setRefundableAmount(line.getRefundableAmount());
            domain.getReturnItems().add(item);
        }
        return domain;
    }

    public static RefundJpaEntity toEntity(Refund domain) {
        RefundJpaEntity entity = new RefundJpaEntity();
        entity.setRefundId(domain.getIdentifier());
        entity.setReturnRequestId(domain.getReturnRequest().getIdentifier());
        entity.setAmount(domain.getAmount());
        entity.setCurrency(CatalogCodes.code(domain.getCurrency()));
        entity.setRefundStatus(CatalogCodes.code(domain.getRefundStatus()));
        entity.setProcessedById(domain.getProcessedBy() == null ? null : domain.getProcessedBy().getUserId());
        entity.setProcessDate(domain.getProcessDate());
        return entity;
    }

    public static Refund toDomain(RefundJpaEntity entity, ReturnRequest returnRequest, Administrator processedBy) {
        Refund domain = new Refund();
        domain.setIdentifier(entity.getRefundId());
        domain.setReturnRequest(returnRequest);
        domain.setAmount(entity.getAmount());
        domain.setCurrency(CatalogCodes.fromCode(Currency.class, entity.getCurrency()));
        domain.setRefundStatus(CatalogCodes.fromCode(RefundStatus.class, entity.getRefundStatus()));
        domain.setProcessedBy(processedBy);
        domain.setProcessDate(entity.getProcessDate());
        return domain;
    }

    private static ReturnItemEmbeddable toEmbeddable(ReturnItem item) {
        ReturnItemEmbeddable line = new ReturnItemEmbeddable();
        line.setVariantId(item.getOrderItem().getVariant().getVariantId());
        line.setQuantity(item.getQuantity());
        line.setRefundableAmount(item.getRefundableAmount());
        return line;
    }
}
