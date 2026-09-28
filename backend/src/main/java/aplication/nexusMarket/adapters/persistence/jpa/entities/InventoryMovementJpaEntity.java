package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
@NoArgsConstructor
public class InventoryMovementJpaEntity {

    @Id
    @Column(name = "movement_id", length = 36)
    private String movementId;

    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    /** InventoryMovementType code. */
    @Column(name = "movement_type", nullable = false, length = 40)
    private String movementType;

    /** Signed only for ADJUSTMENT. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    @Column(name = "performed_by_id", nullable = false, length = 36)
    private String performedById;
}
