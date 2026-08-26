package aplication.nexusMarket.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the participant responsible for the physical operation of warehouses and dispatches.
 *
 * <p>Performs packing, dispatch and transport, and shares inventory administration with the seller.
 * No additional attributes beyond those inherited from {@link User}.
 *
 * <p>Source: Seccion 5; OBJ-10; Seccion 6.1 step 7; Matriz de Responsabilidades.
 */
@Getter
@Setter
@NoArgsConstructor
public class LogisticsOperator extends User {
}
