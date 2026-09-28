package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.Shipment;
import java.util.List;
import java.util.Optional;

/** findById includes the order with its lines and all its shipments, which decide the order's status. */
public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findById(Shipment shipment);

    List<Shipment> findByOrder(Order order);

    void update(Shipment shipment);
}
