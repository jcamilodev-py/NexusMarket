package aplication.nexusMarket.adapters.persistence;

import aplication.nexusMarket.domain.valueobjects.DomainCatalog;

/**
 * Catalogs are stored as their business code, never as ordinals, so the data stays readable without
 * the Java model and survives reordering the catalog's values.
 */
public final class CatalogCodes {

    private CatalogCodes() {
    }

    public static String code(DomainCatalog value) {
        return value == null ? null : value.getCode();
    }

    public static <E extends Enum<E> & DomainCatalog> E fromCode(Class<E> catalog, String code) {
        if (code == null) {
            return null;
        }
        for (E value : catalog.getEnumConstants()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new IllegalStateException("Unknown " + catalog.getSimpleName() + " code stored: " + code);
    }
}
