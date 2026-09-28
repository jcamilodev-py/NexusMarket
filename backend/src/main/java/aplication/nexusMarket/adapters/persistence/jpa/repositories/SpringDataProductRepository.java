package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.ProductJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, String> {

    List<ProductJpaEntity> findByProductStatus(String productStatus);

    Optional<ProductJpaEntity> findByVariants_VariantId(String variantId);

    boolean existsByVariants_Sku(String sku);
}
