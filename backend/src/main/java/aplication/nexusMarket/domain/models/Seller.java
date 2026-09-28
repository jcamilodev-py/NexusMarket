package aplication.nexusMarket.domain.models;

import aplication.nexusMarket.domain.exceptions.InvalidSellerException;
import aplication.nexusMarket.domain.exceptions.InvalidWarehouseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public void validateCommercialIdentity() {
        requireCommercialIdentity(legalBusinessName, taxId);
    }

    /** Returns the names of the attributes that actually changed, so callers can reject a no-op. */
    public List<String> updateCommercialIdentity(String newLegalBusinessName, String newTaxId, String newTradeName) {
        requireCommercialIdentity(newLegalBusinessName, newTaxId);
        List<String> changedFields = new ArrayList<>();
        if (!newLegalBusinessName.equals(legalBusinessName)) {
            changedFields.add("legalBusinessName");
        }
        if (!newTaxId.equals(taxId)) {
            changedFields.add("taxId");
        }
        if (!Objects.equals(newTradeName, tradeName)) {
            changedFields.add("tradeName");
        }
        this.legalBusinessName = newLegalBusinessName;
        this.taxId = newTaxId;
        this.tradeName = newTradeName;
        return changedFields;
    }

    /** The only place where a seller warehouse gets its owner. */
    public void addWarehouse(SellerWarehouse warehouse) {
        if (warehouse == null) {
            throw new InvalidWarehouseException("The warehouse must be provided.");
        }
        warehouse.setOwner(this);
        warehouses.add(warehouse);
    }

    private void requireCommercialIdentity(String legalName, String taxIdentification) {
        if (legalName == null || legalName.isBlank()) {
            throw new InvalidSellerException("Legal business name must not be blank.");
        }
        if (taxIdentification == null || taxIdentification.isBlank()) {
            throw new InvalidSellerException("Tax identification must not be blank.");
        }
    }
}
