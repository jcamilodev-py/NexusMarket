package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a consultation and operational follow-up profile.
 *
 * <p>Holds read-only access over the operational information of the marketplace, which makes
 * {@link Operation} and {@link AuditLog} its primary working material. No additional attributes
 * beyond those inherited from {@link User}.
 *
 * <p>Source: Seccion 5; OBJ-12.
 */
@Getter
@Setter
@NoArgsConstructor
public class Supervisor extends User {
}
