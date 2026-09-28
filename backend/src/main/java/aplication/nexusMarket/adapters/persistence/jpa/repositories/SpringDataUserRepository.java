package aplication.nexusMarket.adapters.persistence.jpa.repositories;

import aplication.nexusMarket.adapters.persistence.jpa.entities.UserJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdentificationNumber(String identificationNumber);
}
