package no.casevault.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import no.casevault.config.JwtProperties;
import no.casevault.model.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    public record TokenClaims(String username, String role, String district) {
    }

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(User user, Instant issuedAt) {
        Instant expiresAt = expirationFor(issuedAt);
        return Jwts.builder()
            .subject(user.getUsername())
            .claim("role", user.getRole().name())
            .claim("district", user.getDistrict().name())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();
    }

    public Instant expirationFor(Instant issuedAt) {
        return issuedAt.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);
    }

    public TokenClaims parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return new TokenClaims(
            claims.getSubject(),
            claims.get("role", String.class),
            claims.get("district", String.class)
        );
    }
}
