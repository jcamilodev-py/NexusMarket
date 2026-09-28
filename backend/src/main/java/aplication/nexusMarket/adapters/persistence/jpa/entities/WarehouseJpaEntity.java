package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Both warehouse kinds in one table; ownerId is only set for seller warehouses. */
@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class WarehouseJpaEntity {

    public static final String MARKETPLACE = "MARKETPLACE";
    public static final String SELLER = "SELLER";

    @Id
    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    @Column(name = "warehouse_type", nullable = false, length = 20)
    private String warehouseType;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "owner_id", length = 36)
    private String ownerId;
}
