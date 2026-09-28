package aplication.nexusMarket.adapters.persistence.jpa.mappers;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.entities.PaymentJpaEntity;
import aplication.nexusMarket.domain.models.Invoice;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Payment;
import aplication.nexusMarket.domain.valueobjects.Currency;
import aplication.nexusMarket.domain.valueobjects.PaymentStatus;

/** Payments and invoices always belong to an order the caller already holds. */
public final class BillingJpaMapper {

    private BillingJpaMapper() {
    }

    public static PaymentJpaEntity toEntity(Payment domain) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setPaymentId(domain.getIdentifier());
        entity.setOrderId(domain.getOrder().getIdentifier());
        entity.setAmount(domain.getAmount());
        entity.setCurrency(CatalogCodes.code(domain.getCurrency()));
        entity.setPaymentStatus(CatalogCodes.code(domain.getPaymentStatus()));
        entity.setPaymentDate(domain.getPaymentDate());
        return entity;
    }

    public static Payment toDomain(PaymentJpaEntity entity, Order order) {
        Payment domain = new Payment();
        domain.setIdentifier(entity.getPaymentId());
        domain.setOrder(order);
        domain.setAmount(entity.getAmount());
        domain.setCurrency(CatalogCodes.fromCode(Currency.class, entity.getCurrency()));
        domain.setPaymentStatus(CatalogCodes.fromCode(PaymentStatus.class, entity.getPaymentStatus()));
        domain.setPaymentDate(entity.getPaymentDate());
        return domain;
    }

    public static InvoiceJpaEntity toEntity(Invoice domain) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setInvoiceId(domain.getIdentifier());
        entity.setOrderId(domain.getOrder().getIdentifier());
        entity.setBuyerId(domain.getBuyer().getUserId());
        entity.setIssueDate(domain.getIssueDate());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setCurrency(CatalogCodes.code(domain.getCurrency()));
        return entity;
    }

    public static Invoice toDomain(InvoiceJpaEntity entity, Order order) {
        Invoice domain = new Invoice();
        domain.setIdentifier(entity.getInvoiceId());
        domain.setOrder(order);
        domain.setBuyer(order.getBuyer());
        domain.setIssueDate(entity.getIssueDate());
        domain.setTotalAmount(entity.getTotalAmount());
        domain.setCurrency(CatalogCodes.fromCode(Currency.class, entity.getCurrency()));
        return domain;
    }
}
