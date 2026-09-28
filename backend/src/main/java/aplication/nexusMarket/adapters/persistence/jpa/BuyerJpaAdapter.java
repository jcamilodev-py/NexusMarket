package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.UserJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataUserRepository;
import aplication.nexusMarket.domain.models.Buyer;
import aplication.nexusMarket.domain.ports.out.BuyerRepositoryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Buyers live in the users table; this adapter reads and writes them with their buyer attributes. */
@Repository
@RequiredArgsConstructor
public class BuyerJpaAdapter implements BuyerRepositoryPort {

    private final SpringDataUserRepository userRepository;

    @Override
    public Buyer save(Buyer buyer) {
        buyer.setUserId(Identifiers.orNew(buyer.getUserId()));
        userRepository.save(UserJpaMapper.toEntity(buyer));
        return buyer;
    }

    @Override
    public Optional<Buyer> findById(Buyer buyer) {
        if (buyer == null || buyer.getUserId() == null) {
            return Optional.empty();
        }
        return userRepository.findById(buyer.getUserId())
                .map(UserJpaMapper::toDomain)
                .filter(Buyer.class::isInstance)
                .map(Buyer.class::cast);
    }

    @Override
    public void update(Buyer buyer) {
        userRepository.save(UserJpaMapper.toEntity(buyer));
    }
}
