package aplication.nexusMarket.adapters.persistence;

import java.util.UUID;

/** Identifiers are UUIDs assigned by the adapter on first save, as in the banking reference. */
public final class Identifiers {

    private Identifiers() {
    }

    public static String orNew(String identifier) {
        return identifier == null || identifier.isBlank() ? UUID.randomUUID().toString() : identifier;
    }
}
