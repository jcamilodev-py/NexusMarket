package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.CatalogCodes;
import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.entities.OrderJpaEntity;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.OrderJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.ShipmentJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataOrderRepository;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataShipmentRepository;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.models.Inventory;
import aplication.nexusMarket.domain.models.LogisticsOperator;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.ProductVariant;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import aplication.nexusMarket.domain.ports.out.InventoryRepositoryPort;
import aplication.nexusMarket.domain.ports.out.InvoiceRepositoryPort;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.ports.out.PaymentRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ProductRepositoryPort;
import aplication.nexusMarket.domain.valueobjects.ReportPeriod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Assembles the whole order: buyer, frozen lines with their variants and source inventory, payments,
 * invoice and shipments. Shipments are read from their table here rather than through their port,
 * because the shipment adapter itself resolves orders and the two ports would depend on each other.
 */
@Repository
@RequiredArgsConstructor
public class OrderJpaAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataShipmentRepository shipmentRepository;
    private final BuyerRepositoryPort buyerRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final InventoryRepositoryPort inventoryRepositoryPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public Order save(Order order) {
        order.setIdentifier(Identifiers.orNew(order.getIdentifier()));
        orderRepository.save(OrderJpaMapper.toEntity(order));
        return order;
    }

    @Override
    public Optional<Order> findById(Order order) {
        if (order == null || order.getIdentifier() == null) {
            return Optional.empty();
        }
        return orderRepository.findById(order.getIdentifier()).map(this::assemble);
    }

    @Override
    public List<Order> findByBuyer(User buyer) {
        return assembleAll(orderRepository.findByBuyerId(buyer.getUserId()));
    }

    @Override
    public List<Order> findBySeller(User seller) {
        return assembleAll(orderRepository.findBySellerId(seller.getUserId()));
    }

    @Override
    public List<Order> findByStatus(Order criteria) {
        if (criteria == null || criteria.getOrderStatus() == null) {
            return assembleAll(orderRepository.findAll());
        }
        return assembleAll(orderRepository.findByOrderStatus(CatalogCodes.code(criteria.getOrderStatus())));
    }

    @Override
    public List<Order> findByPeriod(ReportPeriod period) {
        return assembleAll(orderRepository.findByCreationDateGreaterThanEqualAndCreationDateLessThan(
                period.from().atStartOfDay(), period.to().plusDays(1).atStartOfDay()));
    }

    @Override
    public void update(Order order) {
        orderRepository.save(OrderJpaMapper.toEntity(order));
    }

    private List<Order> assembleAll(List<OrderJpaEntity> entities) {
        return entities.stream().map(this::assemble).toList();
    }

    private Order assemble(OrderJpaEntity entity) {
        Buyer buyerReference = new Buyer();
        buyerReference.setUserId(entity.getBuyerId());
        Map<String, ProductVariant> variants = new HashMap<>();
        Map<String, Inventory> inventories = new HashMap<>();

        Order order = OrderJpaMapper.toDomain(entity,
                buyerRepositoryPort.findById(buyerReference).orElse(null),
                variantId -> variants.computeIfAbsent(variantId, this::variant),
                inventoryId -> inventories.computeIfAbsent(inventoryId, this::inventory));
        order.setPayments(new ArrayList<>(paymentRepositoryPort.findByOrder(order)));
        invoiceRepositoryPort.findByOrder(order).ifPresent(order::setInvoice);
        order.setShipments(new ArrayList<>(shipmentRepository.findByOrderId(order.getIdentifier()).stream()
                .map(shipment -> ShipmentJpaMapper.toDomain(shipment, order,
                        referenceResolver.warehouse(shipment.getOriginWarehouseId()),
                        referenceResolver.user(shipment.getLogisticsOperatorId(), LogisticsOperator.class)))
                .toList()));
        return order;
    }

    private ProductVariant variant(String variantId) {
        ProductVariant variantReference = new ProductVariant();
        variantReference.setVariantId(variantId);
        return productRepositoryPort.findVariantById(variantReference).orElse(null);
    }

    private Inventory inventory(String inventoryId) {
        Inventory inventoryReference = new Inventory();
        inventoryReference.setIdentifier(inventoryId);
        return inventoryRepositoryPort.findById(inventoryReference).orElse(null);
    }
}
