package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.jpa.mappers.UserJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataUserRepository;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataWarehouseRepository;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.models.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Rebuilds user and warehouse references from their identifiers. These two are resolved here rather
 * than through their ports because User and Warehouse are abstract - there is no instance to pass as
 * a lookup key - and because neither holds further references to resolve.
 */
@Component
@RequiredArgsConstructor
public class JpaReferenceResolver {

    private final SpringDataUserRepository userRepository;
    private final SpringDataWarehouseRepository warehouseRepository;

    public User user(String userId) {
        return userId == null ? null : userRepository.findById(userId).map(UserJpaMapper::toDomain).orElse(null);
    }

    public <T extends User> T user(String userId, Class<T> specialization) {
        User user = user(userId);
        return specialization.isInstance(user) ? specialization.cast(user) : null;
    }

    public Warehouse warehouse(String warehouseId) {
        return warehouseId == null ? null : warehouseRepository.findById(warehouseId)
                .map(entity -> WarehouseJpaMapper.toDomain(entity, user(entity.getOwnerId(), Seller.class)))
                .orElse(null);
    }
}
