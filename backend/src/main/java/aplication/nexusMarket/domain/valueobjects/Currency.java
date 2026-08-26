package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents a monetary currency supported by the marketplace.
 *
 * <p>Every entity holding a monetary amount also holds its currency: Product, Order, Payment,
 * Invoice and Refund. A BigDecimal with no denomination becomes ambiguous the moment more than one
 * currency exists, and a refunded amount must be provably the same amount that was charged.
 *
 * <p>Source: inferred. The specification enumerates no currencies, so the catalog is seeded with the
 * same values as the banking reference: supporting an additional currency is a catalog entry, not a
 * change to the domain model.
 */
@Getter
public enum Currency implements DomainCatalog {

    COP("COP", "Colombian Peso", "Currency of Colombia.", "COP", "$"),
    USD("USD", "United States Dollar", "Currency of the United States.", "USD", "$"),
    EUR("EUR", "Euro", "Currency of the euro area.", "EUR", "€");

    private final String code;
    private final String name;
    private final String description;

    /** ISO 4217 currency code. */
    private final String isoCode;

    /** Currency symbol used for display. */
    private final String symbol;

    Currency(String code, String name, String description, String isoCode, String symbol) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.isoCode = isoCode;
        this.symbol = symbol;
    }
}
