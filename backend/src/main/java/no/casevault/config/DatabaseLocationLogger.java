package no.casevault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@Profile("!test")
public class DatabaseLocationLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseLocationLogger.class);

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Using H2 database at resolved path: {} (from working directory {}, datasource URL: {})",
            Path.of("./data").toAbsolutePath().normalize(),
            Path.of("").toAbsolutePath(),
            datasourceUrl);
    }
}
