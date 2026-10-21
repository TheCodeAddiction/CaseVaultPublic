package no.casevault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "casevault.storage")
public record StorageProperties(String directory, long maxFileSizeBytes) {
}
