package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.Order;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.ReportPeriod;
import java.util.List;
import java.util.Optional;

/**
 * Lookups return the order with its buyer, lines (variant, product, seller, source inventory),
 * payments, invoice and shipments. findByStatus returns every order when no status is given.
 */
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(Order order);

    List<Order> findByBuyer(User buyer);

    List<Order> findBySeller(User seller);

    List<Order> findByStatus(Order criteria);

    List<Order> findByPeriod(ReportPeriod period);

    void update(Order order);
}
