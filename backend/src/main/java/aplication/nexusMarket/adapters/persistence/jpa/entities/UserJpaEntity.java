package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Every user in one table: the role decides which specialization the mapper rebuilds (RG-02), and
 * authenticating never needs a join. Buyer and seller columns stay empty for the other roles.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "identification_number", nullable = false, unique = true, length = 40)
    private String identificationNumber;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** SystemRole code. */
    @Column(name = "role", nullable = false, length = 40)
    private String role;

    /** UserStatus code. */
    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "primary_address", length = 255)
    private String primaryAddress;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "buyer_additional_addresses", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "address", length = 255)
    private List<String> additionalAddresses = new ArrayList<>();

    /** BuyerCommercialStatus code. */
    @Column(name = "commercial_status", length = 40)
    private String commercialStatus;

    @Column(name = "active_cart_id", length = 36)
    private String activeCartId;

    @Column(name = "legal_business_name", length = 200)
    private String legalBusinessName;

    @Column(name = "tax_id", length = 40)
    private String taxId;

    @Column(name = "trade_name", length = 150)
    private String tradeName;
}
