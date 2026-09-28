package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The shipment's lines are stored as the variants they carry; the adapter matches them to the order's
 * own lines, so the Domain works with the same OrderItem objects the order holds.
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class ShipmentJpaEntity {

    @Id
    @Column(name = "shipment_id", length = 36)
    private String shipmentId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "origin_warehouse_id", nullable = false, length = 36)
    private String originWarehouseId;

    @Column(name = "logistics_operator_id", nullable = false, length = 36)
    private String logisticsOperatorId;

    /** ShipmentStatus code. */
    @Column(name = "shipment_status", nullable = false, length = 40)
    private String shipmentStatus;

    @Column(name = "dispatch_date")
    private LocalDateTime dispatchDate;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shipment_items", joinColumns = @JoinColumn(name = "shipment_id"))
    @Column(name = "variant_id", nullable = false, length = 36)
    private List<String> variantIds = new ArrayList<>();
}
