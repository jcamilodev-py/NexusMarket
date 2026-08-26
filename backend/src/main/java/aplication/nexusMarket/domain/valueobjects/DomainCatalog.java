package aplication.nexusMarket.domain.valueobjects;

/**
 * Represents a generic business catalog used throughout the NexusMarket domain.
 *
 * <p>All controlled business values inherit from this abstraction, which guarantees a consistent
 * structure: every catalog value carries a business {@code code}, a human-readable {@code name},
 * and a business {@code description}.
 *
 * <p>Catalog values are immutable, are compared by value rather than by object identity, and must
 * never be replaced by arbitrary strings throughout the application.
 *
 * @see <a href="../../../../../../../../SDD/domain/Domain%20Value%20Objects.md">Domain Value Objects</a>
 */
public interface DomainCatalog {

    /** Unique business identifier of the catalog value. */
    String getCode();

    /** Human-readable name displayed within the application. */
    String getName();

    /** Business definition of the catalog value. */
    String getDescription();
}
