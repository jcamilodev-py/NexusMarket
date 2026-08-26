package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the current stage of a return request.
 *
 * <p>The return concerns the product being sent back; the money movement that follows an approved
 * return is represented separately by {@link RefundStatus}.
 *
 * <p>Source: OBJ-11; Matriz de Responsabilidades. Values inferred following the
 * request-decision-closure pattern.
 */
@Getter
public enum ReturnStatus implements DomainCatalog {

    REQUESTED("REQUESTED", "Requested",
            "Return request created by the buyer, awaiting review."),
    APPROVED("APPROVED", "Approved",
            "Return accepted; it originates a Refund and, for physical items, a stock return."),
    REJECTED("REJECTED", "Rejected",
            "Return request denied. No refund is originated."),
    COMPLETED("COMPLETED", "Completed",
            "Returned product received and processed; the request is closed.");

    private final String code;
    private final String name;
    private final String description;

    ReturnStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
