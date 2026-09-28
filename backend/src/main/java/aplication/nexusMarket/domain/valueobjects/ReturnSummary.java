package aplication.nexusMarket.domain.valueobjects;

import aplication.nexusMarket.domain.models.Refund;
import aplication.nexusMarket.domain.models.ReturnRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One line of the returns and refunds report. Only PROCESSED refunds count as refunded money: a
 * pending refund has not left the marketplace and a rejected one never will. Not a catalog.
 */
public record ReturnSummary(ReturnStatus status, Currency currency, int requestCount, BigDecimal refundedAmount) {

    public static List<ReturnSummary> summarize(List<ReturnRequest> requests, List<Refund> refunds) {
        Map<ReturnStatus, Map<Currency, List<ReturnRequest>>> grouped = requests.stream()
                .collect(Collectors.groupingBy(ReturnRequest::getReturnStatus,
                        Collectors.groupingBy(request -> request.getOrder().getCurrency())));
        return grouped.entrySet().stream()
                .flatMap(byStatus -> byStatus.getValue().entrySet().stream()
                        .map(byCurrency -> new ReturnSummary(byStatus.getKey(), byCurrency.getKey(),
                                byCurrency.getValue().size(), refundedAmount(byCurrency.getValue(), refunds))))
                .toList();
    }

    private static BigDecimal refundedAmount(List<ReturnRequest> requests, List<Refund> refunds) {
        Set<String> requestIds = requests.stream().map(ReturnRequest::getIdentifier).collect(Collectors.toSet());
        return refunds.stream()
                .filter(refund -> RefundStatus.PROCESSED.equals(refund.getRefundStatus()))
                .filter(refund -> requestIds.contains(refund.getReturnRequest().getIdentifier()))
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
