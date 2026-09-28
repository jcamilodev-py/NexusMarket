package aplication.nexusMarket.domain.valueobjects;

import aplication.nexusMarket.domain.models.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * One line of the sales report: orders of one status and currency. Amounts are grouped by currency
 * too, since adding different currencies would be meaningless. Not a catalog.
 */
public record SalesSummary(OrderStatus status, Currency currency, int orderCount, BigDecimal totalAmount) {

    public static List<SalesSummary> summarize(List<Order> orders) {
        Map<OrderStatus, Map<Currency, List<Order>>> grouped = orders.stream()
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.groupingBy(Order::getCurrency)));
        return grouped.entrySet().stream()
                .flatMap(byStatus -> byStatus.getValue().entrySet().stream()
                        .map(byCurrency -> new SalesSummary(byStatus.getKey(), byCurrency.getKey(),
                                byCurrency.getValue().size(),
                                byCurrency.getValue().stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))))
                .toList();
    }
}
