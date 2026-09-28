package aplication.nexusMarket.adapters.security;

import aplication.nexusMarket.domain.models.User;
import aplication.nexusMarket.domain.ports.out.JwtServicePort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues the token with the claims the input adapter needs to rebuild the requesting user. It carries
 * no status: authorization always reloads it, so a user blocked after login is stopped at once.
 */
@Component
public class JwtServiceAdapter implements JwtServicePort {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtServiceAdapter(@Value("${nexusmarket.jwt.secret}") String secret,
                             @Value("${nexusmarket.jwt.expiration-ms}") long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    @Override
    public String generateToken(User user) {
        Date issuedAt = new Date();
        return Jwts.builder()
                .subject(user.getUserId())
                .claim("userId", user.getUserId())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().getCode())
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + expirationMillis))
                .signWith(signingKey)
                .compact();
    }
}
