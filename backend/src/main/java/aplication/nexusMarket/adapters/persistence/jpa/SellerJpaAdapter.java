package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.UserJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataUserRepository;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataWarehouseRepository;
import aplication.nexusMarket.domain.models.Seller;
import aplication.nexusMarket.domain.models.SellerWarehouse;
import aplication.nexusMarket.domain.ports.out.SellerRepositoryPort;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Sellers live in the users table. Their warehouses are loaded with them and owned by the very same
 * seller instance; their products are not loaded (Domain Model, Seller).
 */
@Repository
@RequiredArgsConstructor
public class SellerJpaAdapter implements SellerRepositoryPort {

    private final SpringDataUserRepository userRepository;
    private final SpringDataWarehouseRepository warehouseRepository;

    @Override
    public Seller save(Seller seller) {
        seller.setUserId(Identifiers.orNew(seller.getUserId()));
        userRepository.save(UserJpaMapper.toEntity(seller));
        return seller;
    }

    @Override
    public Optional<Seller> findById(Seller seller) {
        if (seller == null || seller.getUserId() == null) {
            return Optional.empty();
        }
        return userRepository.findById(seller.getUserId())
                .map(UserJpaMapper::toDomain)
                .filter(Seller.class::isInstance)
                .map(Seller.class::cast)
                .map(this::withWarehouses);
    }

    @Override
    public void update(Seller seller) {
        userRepository.save(UserJpaMapper.toEntity(seller));
    }

    private Seller withWarehouses(Seller seller) {
        seller.setWarehouses(new ArrayList<>(warehouseRepository.findByOwnerId(seller.getUserId()).stream()
                .map(entity -> (SellerWarehouse) WarehouseJpaMapper.toDomain(entity, seller))
                .toList()));
        return seller;
    }
}
