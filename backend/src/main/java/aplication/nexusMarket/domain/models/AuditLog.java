package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import aplication.nexusMarket.domain.valueobjects.SystemRole;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the immutable audit trail of the marketplace.
 *
 * <p>Each record stores historical information about a significant business operation, together with
 * the role held by the user at the moment of execution. Records are intended to be persisted in a
 * NoSQL database to support flexible, operation-specific details and long-term traceability.
 *
 * <p>Together with {@link Operation}, this entity satisfies OBJ-12 and the traceability commitment
 * of Seccion 1, both of which describe cross-cutting administrative traceability rather than the
 * state of a single business transaction.
 *
 * <p>Business rules: audit records are immutable and append-only; a record cannot be deleted after
 * persistence; every significant business operation must produce one; userRole must represent the
 * role applicable at the time the operation was performed, not the current role of the user.
 *
 * <p>Source: OBJ-12; Seccion 1; Seccion 5.
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    private String auditId;

    private OperationType operationType;

    private LocalDateTime operationDate;

    private User performedBy;

    /** Role of the user at the time of execution, not their current role. */
    private SystemRole userRole;

    private AffectedEntityType affectedEntityType;

    private String affectedEntityId;

    /** Flexible document containing operation-specific information. */
    private Map<String, Object> details = new HashMap<>();
}
