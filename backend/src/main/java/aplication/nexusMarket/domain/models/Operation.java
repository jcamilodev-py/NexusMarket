package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.valueobjects.AffectedEntityType;
import aplication.nexusMarket.domain.valueobjects.OperationType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a significant business action executed within the marketplace.
 *
 * <p>Operations provide traceability between users, business entities and audit records. An
 * operation represents an event that occurred; it is distinct from the current status of the
 * affected entity. For example {@code Order.orderStatus = PAID} is the current state of the order,
 * while {@code Operation.operationType = ORDER_PAYMENT_CONFIRMATION} is the action that caused it.
 *
 * <p><b>Typed reference instead of a common abstract root.</b> The banking reference points its
 * Operation at a BankingProduct abstract root. NexusMarket has no equivalent business concept, so
 * the affected entity is qualified by {@link AffectedEntityType} plus its identifier, which keeps
 * the reference typed and domain-controlled without inventing a false hierarchy.
 *
 * <p>Business rules: every significant business action must generate an Operation; every Operation
 * must be executed by an authenticated User (RG-01); every Operation must be recorded in the
 * {@link AuditLog}; an Operation is never modified, because the event itself does not change.
 *
 * <p>Source: OBJ-12; Seccion 1; RG-01.
 */
@Getter
@Setter
@NoArgsConstructor
public class Operation {

    private String operationId;

    private OperationType operationType;

    private LocalDateTime executionDate;

    private User performedBy;

    /** Type of business entity affected by the operation. */
    private AffectedEntityType affectedEntityType;

    /** Identifier of the specific instance affected by the operation. */
    private String affectedEntityId;
}
