package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.ReturnStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the request of a buyer to return one or more purchased items from an order.
 *
 * <p>The request states exactly which order lines are being returned and in what quantity, through
 * its {@link ReturnItem} lines. Without that detail the amount to be refunded could not be
 * determined, since a buyer may return only part of an order.
 *
 * <p>Business rules: may only be created by the buyer who owns the order, and only over an order
 * that has been delivered; the returned quantity of a line may never exceed the quantity purchased,
 * discounting quantities already returned; an approved request over physical items generates an
 * InventoryMovement of type RETURN; it originates a {@link Refund} only when approved.
 *
 * <p>Source: OBJ-11; Matriz de Responsabilidades; DOMINIO 6.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequest {

    private String identifier;

    private Order order;

    /** Buyer who requested the return; must be the buyer of the order. */
    private Buyer requestedBy;

    /** Lines being returned. At least one element. Inferred from "uno o mas productos". */
    private List<ReturnItem> returnItems = new ArrayList<>();

    private String reason;

    private LocalDateTime requestDate;

    private ReturnStatus returnStatus;
}
