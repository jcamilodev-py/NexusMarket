package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.valueobjects.Credentials;

public interface PasswordServicePort {

    String encode(Credentials credentials);

    boolean matches(Credentials credentials, User user);
}
