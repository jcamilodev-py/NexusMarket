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
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequestJpaEntity {

    @Id
    @Column(name = "return_request_id", length = 36)
    private String returnRequestId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "requested_by_id", nullable = false, length = 36)
    private String requestedById;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    /** ReturnStatus code. */
    @Column(name = "return_status", nullable = false, length = 40)
    private String returnStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "return_items", joinColumns = @JoinColumn(name = "return_request_id"))
    private List<ReturnItemEmbeddable> items = new ArrayList<>();
}
