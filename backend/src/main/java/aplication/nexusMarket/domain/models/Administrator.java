package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the participant responsible for administering sellers and warehouses, including seller
 * onboarding, and for executing refunds.
 *
 * <p>No additional attributes beyond those inherited from {@link User}.
 *
 * <p>Source: Seccion 5; DOMINIO 3; DOMINIO 4; Matriz de Responsabilidades.
 */
@Getter
@Setter
@NoArgsConstructor
public class Administrator extends User {
}
