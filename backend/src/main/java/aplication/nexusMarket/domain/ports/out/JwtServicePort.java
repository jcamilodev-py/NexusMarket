package aplication.nexusMarket.domain.ports.out;

import aplication.nexusMarket.domain.models.User;

/**
 * Exposes only Domain types: validating a token and rebuilding the requesting User from it belong
 * to the input adapter, before the Domain is reached.
 */
public interface JwtServicePort {

    String generateToken(User user);
}
