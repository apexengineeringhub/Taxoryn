package com.taxoryn.module.gov.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Enterprise configuration properties for the Government Integration Framework,
 * failure recovery, outbox background processing, and reconciliation schedulers.
 */
@Data
@Component
@ConfigurationProperties(prefix = "taxoryn.gov")
public class GovProductionProperties {

    /**
     * Master switch for government integration framework.
     * In production, defaults to false unless explicitly enabled.
     */
    private Integration integration = new Integration();

    /**
     * Outbox processing settings.
     */
    private Outbox outbox = new Outbox();

    /**
     * Background reconciliation scheduler settings.
     */
    private Reconciliation reconciliation = new Reconciliation();

    /**
     * Encryption settings for credentials at rest.
     */
    private Encryption encryption = new Encryption();

    @Data
    public static class Integration {
        private boolean enabled = true;
        private boolean mockEnabled = true;
    }

    @Data
    public static class Outbox {
        private boolean enabled = true;
        private int batchSize = 20;
        private long staleThresholdSeconds = 300;
        private long fixedDelayMs = 5000;
        private long staleCheckDelayMs = 30000;
    }

    @Data
    public static class Reconciliation {
        private boolean enabled = true;
        private int batchSize = 20;
        private long fixedRateMs = 60000;
    }

    @Data
    public static class Encryption {
        private String secret = "taxoryn-gov-secure-credential-secret-key-32bytes";
    }
}
