package aplication.nexusMarket.adapters.persistence.mongodb.documents;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The performer is denormalized - identifier, name and role at that moment - so the trail stays
 * readable even if the user changes later, and reading it never touches MySQL.
 */
@Document(collection = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogDocument {

    @Id
    private String auditId;

    /** OperationType code. */
    private String operationType;

    private LocalDateTime operationDate;

    private String performedByUserId;

    private String performedByFullName;

    /** SystemRole code held when the operation was performed. */
    private String userRole;

    /** AffectedEntityType code. */
    private String affectedEntityType;

    private String affectedEntityId;

    private Map<String, Object> details = new HashMap<>();
}
