package no.casevault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "casevault.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
