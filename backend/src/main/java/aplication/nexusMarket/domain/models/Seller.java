package aplication.nexusMarket.domain.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a vendor responsible for registering and managing their own products.
 *
 * <p>Sellers cannot self-register; they are onboarded exclusively by an {@link Administrator},
 * together with their first warehouse, so {@code warehouses} always holds at least one element.
 *
 * <p>The commercial identity attributes are inferred by analogy with the BusinessCustomer of the
 * banking reference: a marketplace seller sells to third parties and must be identifiable as a
 * fiscal and commercial entity.
 *
 * <p>Source: DOMINIO 3; OBJ-02; Seccion 6.1 step 1; Matriz de Responsabilidades.
 */
@Getter
@Setter
@NoArgsConstructor
public class Seller extends User {

    /** Legal or registered name of the business. Inferred. */
    private String legalBusinessName;

    /** Tax identification number (NIT/RUT or equivalent). Inferred. */
    private String taxId;

    /** Public-facing storefront name. Optional; may differ from the legal name. */
    private String tradeName;

    /** Warehouses owned by the seller. At least one from the moment of onboarding. */
    private List<SellerWarehouse> warehouses = new ArrayList<>();

    /** Not populated by default; loaded on demand by the corresponding consultation service. */
    private List<Product> products = new ArrayList<>();
}
