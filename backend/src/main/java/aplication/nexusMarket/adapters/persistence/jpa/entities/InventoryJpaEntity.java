package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One record per variant and warehouse (DOMINIO 6), enforced here as well as in the Domain. */
@Entity
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "warehouse_id"}))
@Getter
@Setter
@NoArgsConstructor
public class InventoryJpaEntity {

    @Id
    @Column(name = "inventory_id", length = 36)
    private String inventoryId;

    @Column(name = "variant_id", nullable = false, length = 36)
    private String variantId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    /** InventoryStatus code. */
    @Column(name = "inventory_status", nullable = false, length = 40)
    private String inventoryStatus;
}
