package aplication.nexusMarket.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** returnRequestId is unique: an approved return originates exactly one refund. */
@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
public class RefundJpaEntity {

    @Id
    @Column(name = "refund_id", length = 36)
    private String refundId;

    @Column(name = "return_request_id", nullable = false, unique = true, length = 36)
    private String returnRequestId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Currency code. */
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    /** RefundStatus code. */
    @Column(name = "refund_status", nullable = false, length = 40)
    private String refundStatus;

    @Column(name = "processed_by_id", length = 36)
    private String processedById;

    @Column(name = "process_date")
    private LocalDateTime processDate;
}
