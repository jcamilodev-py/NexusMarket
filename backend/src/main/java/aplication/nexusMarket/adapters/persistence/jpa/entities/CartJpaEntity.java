package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
public class CartJpaEntity {

    @Id
    @Column(name = "cart_id", length = 36)
    private String cartId;

    @Column(name = "buyer_id", nullable = false, length = 36)
    private String buyerId;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    private List<CartItemEmbeddable> items = new ArrayList<>();
}
