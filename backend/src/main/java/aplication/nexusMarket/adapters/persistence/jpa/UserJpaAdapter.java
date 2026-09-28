package aplication.nexusMarket.adapters.persistence.jpa;

import aplication.nexusMarket.adapters.persistence.Identifiers;
import aplication.nexusMarket.adapters.persistence.jpa.mappers.UserJpaMapper;
import aplication.nexusMarket.adapters.persistence.jpa.repositories.SpringDataUserRepository;
import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.UserRepositoryPort;
import aplication.nexusMarket.domain.valueobjects.Credentials;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Users of every role, in MySQL. save returns the same instance it receives, now identified, so the
 * services keep working on the object graph they built.
 */
@Repository
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository userRepository;

    @Override
    public User save(User user) {
        user.setUserId(Identifiers.orNew(user.getUserId()));
        userRepository.save(UserJpaMapper.toEntity(user));
        return user;
    }

    @Override
    public Optional<User> findById(User user) {
        if (user == null || user.getUserId() == null) {
            return Optional.empty();
        }
        return userRepository.findById(user.getUserId()).map(UserJpaMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Credentials credentials) {
        if (credentials == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(credentials.email()).map(UserJpaMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(User user) {
        return user != null && user.getEmail() != null && userRepository.existsByEmail(user.getEmail());
    }

    @Override
    public boolean existsByIdentificationNumber(User user) {
        return user != null && user.getIdentificationNumber() != null
                && userRepository.existsByIdentificationNumber(user.getIdentificationNumber());
    }

    @Override
    public void update(User user) {
        userRepository.save(UserJpaMapper.toEntity(user));
    }
}
