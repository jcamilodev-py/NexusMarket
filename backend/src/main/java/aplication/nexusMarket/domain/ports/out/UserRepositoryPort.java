package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.Credentials;
import java.util.Optional;

/** Lookups return the concrete User specialization that matches the stored role. */
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(User user);

    Optional<User> findByEmail(Credentials credentials);

    boolean existsByEmail(User user);

    boolean existsByIdentificationNumber(User user);

    void update(User user);
}
