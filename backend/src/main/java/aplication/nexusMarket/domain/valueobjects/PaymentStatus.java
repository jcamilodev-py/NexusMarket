package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the state of a payment attempt registered against an order.
 *
 * <p>Each attempt is its own {@code Payment} entity, so a rejected attempt remains as a historical
 * record rather than being overwritten by a later retry. An order moves to PAID only when one of
 * its payment attempts reaches APPROVED.
 *
 * <p>REJECTED and FAILED are distinguished because they carry different business meanings: a
 * commercial refusal is a decision, whereas a technical failure is an incident.
 *
 * <p>Source: DOMINIO 7; Seccion 6.1 step 6. Values inferred.
 */
@Getter
public enum PaymentStatus implements DomainCatalog {

    PENDING("PENDING", "Pending",
            "Payment attempt registered, awaiting validation."),
    APPROVED("APPROVED", "Approved",
            "Payment validated successfully; the order moves to PAID."),
    REJECTED("REJECTED", "Rejected",
            "Payment refused during validation. The order is unaffected and a new attempt may be "
                    + "registered."),
    FAILED("FAILED", "Failed",
            "Payment could not be completed due to a processing error rather than a refusal.");

    private final String code;
    private final String name;
    private final String description;

    PaymentStatus(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
