package aplication.nexusMarket.domain.valueobjects;

import lombok.Getter;

/**
 * Represents the type of change applied to an inventory record.
 *
 * <p>Inventory movements are the historical record of every stock variation, while
 * {@code Inventory} holds the resulting current quantities.
 *
 * <p>Source: DOMINIO 6 - "Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolucion".
 */
@Getter
public enum InventoryMovementType implements DomainCatalog {

    INBOUND("INBOUND", "Inbound",
            "Stock entering the warehouse."),
    RESERVATION("RESERVATION", "Reservation",
            "Stock reserved for a pending order."),
    SALE_OUTBOUND("SALE_OUTBOUND", "Sale Outbound",
            "Stock leaving the warehouse due to a confirmed sale."),
    ADJUSTMENT("ADJUSTMENT", "Adjustment",
            "Manual correction of the stock quantity."),
    RETURN("RETURN", "Return",
            "Stock re-entering due to an approved product return.");

    private final String code;
    private final String name;
    private final String description;

    InventoryMovementType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
