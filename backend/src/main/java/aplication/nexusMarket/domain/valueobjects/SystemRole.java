package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the responsibilities and permissions assigned to a participant within the marketplace.
 *
 * <p>Each user holds exactly one role (RG-02), and that role determines which {@code User}
 * specialization applies.
 *
 * <p>Source: Seccion 5. Participantes del Negocio; RG-02.
 */
@Getter
public enum SystemRole implements DomainCatalog {

    BUYER("BUYER", "Buyer",
            "Purchases products published on the marketplace."),
    SELLER("SELLER", "Seller",
            "Registers and manages their own products."),
    LOGISTICS_OPERATOR("LOGISTICS_OPERATOR", "Logistics Operator",
            "Manages the physical operation of warehouses and dispatches."),
    ADMINISTRATOR("ADMINISTRATOR", "Administrator",
            "Manages sellers, warehouses, and refunds."),
    SUPERVISOR("SUPERVISOR", "Supervisor",
            "Consultation and operational follow-up profile, with read-only access.");

    private final String code;
    private final String name;
    private final String description;

    SystemRole(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
