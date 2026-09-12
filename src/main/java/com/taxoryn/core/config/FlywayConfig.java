package com.taxoryn.core.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy for the Taxoryn Platform.
 * <p>
 * Performs automatic Flyway repair prior to migration execution to safely reconcile
 * migration checksum history across staging, demo, and production PostgreSQL databases
 * without requiring manual intervention, direct SQL updates, or database recreation.
 */
@Slf4j
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Executing Flyway repair to synchronize schema history checksums with canonical classpath definitions...");
            try {
                flyway.repair();
            } catch (Exception ex) {
                log.warn("Flyway repair encountered non-critical exception: {}", ex.getMessage());
            }
            log.info("Executing Flyway database migrations...");
            flyway.migrate();
            log.info("Flyway database migrations completed successfully.");
        };
    }
}