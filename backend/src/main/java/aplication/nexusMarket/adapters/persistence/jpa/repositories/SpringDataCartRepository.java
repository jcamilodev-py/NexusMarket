package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.CartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCartRepository extends JpaRepository<CartJpaEntity, String> {
}
