package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Stored in MySQL next to the business change it traces, so both share the same transaction. */
@Entity
@Table(name = "operations")
@Getter
@Setter
@NoArgsConstructor
public class OperationJpaEntity {

    @Id
    @Column(name = "operation_id", length = 36)
    private String operationId;

    /** OperationType code. */
    @Column(name = "operation_type", nullable = false, length = 60)
    private String operationType;

    @Column(name = "execution_date", nullable = false)
    private LocalDateTime executionDate;

    @Column(name = "performed_by_id", nullable = false, length = 36)
    private String performedById;

    /** AffectedEntityType code. */
    @Column(name = "affected_entity_type", nullable = false, length = 40)
    private String affectedEntityType;

    @Column(name = "affected_entity_id", nullable = false, length = 36)
    private String affectedEntityId;
}
