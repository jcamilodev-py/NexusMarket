package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.OrderJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, String> {

    List<OrderJpaEntity> findByBuyerId(String buyerId);

    List<OrderJpaEntity> findByOrderStatus(String orderStatus);

    List<OrderJpaEntity> findByCreationDateGreaterThanEqualAndCreationDateLessThan(LocalDateTime from, LocalDateTime to);

    @Query("select distinct o from OrderJpaEntity o join o.items i where i.variantId in "
            + "(select v.variantId from ProductJpaEntity p join p.variants v where p.sellerId = :sellerId)")
    List<OrderJpaEntity> findBySellerId(@Param("sellerId") String sellerId);
}
