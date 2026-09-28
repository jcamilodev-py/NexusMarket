package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.ShipmentJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataShipmentRepository;
import aplication.nexusMarket.domain.models.LogisticsOperator;
import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Shipment;
import aplication.nexusMarket.domain.ports.out.OrderRepositoryPort;
import aplication.nexusMarket.domain.ports.out.ShipmentRepositoryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ShipmentJpaAdapter implements ShipmentRepositoryPort {

    private final SpringDataShipmentRepository shipmentRepository;
    private final OrderRepositoryPort orderRepositoryPort;
    private final JpaReferenceResolver referenceResolver;

    @Override
    public Shipment save(Shipment shipment) {
        shipment.setIdentifier(Identifiers.orNew(shipment.getIdentifier()));
        shipmentRepository.save(ShipmentJpaMapper.toEntity(shipment));
        return shipment;
    }

    /**
     * Returns the instance held by the loaded order, not a copy: dispatching or delivering it must be
     * visible to Order.isReadyForDelivery, which reads the order's own shipments.
     */
    @Override
    public Optional<Shipment> findById(Shipment shipment) {
        if (shipment == null || shipment.getIdentifier() == null) {
            return Optional.empty();
        }
        return shipmentRepository.findById(shipment.getIdentifier()).flatMap(entity -> {
            Order orderReference = new Order();
            orderReference.setIdentifier(entity.getOrderId());
            return orderRepositoryPort.findById(orderReference)
                    .flatMap(order -> order.getShipments().stream()
                            .filter(candidate -> entity.getShipmentId().equals(candidate.getIdentifier()))
                            .findFirst());
        });
    }

    @Override
    public List<Shipment> findByOrder(Order order) {
        return shipmentRepository.findByOrderId(order.getIdentifier()).stream()
                .map(entity -> ShipmentJpaMapper.toDomain(entity, order,
                        referenceResolver.warehouse(entity.getOriginWarehouseId()),
                        referenceResolver.user(entity.getLogisticsOperatorId(), LogisticsOperator.class)))
                .toList();
    }

    @Override
    public void update(Shipment shipment) {
        shipmentRepository.save(ShipmentJpaMapper.toEntity(shipment));
    }
}
