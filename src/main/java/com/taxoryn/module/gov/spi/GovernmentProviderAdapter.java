package com.taxoryn.module.gov.spi;

import com.taxoryn.module.gov.dto.GovHandshakeRequest;
import com.taxoryn.module.gov.dto.GovHandshakeResult;
import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.model.GovProviderHealth;
import com.taxoryn.module.gov.model.GovProviderType;

/**
 * Service Provider Interface (SPI) for government subsystem provider adapters.
 * Adapter implementations must be decoupled from Taxoryn internal domain entities.
 */
public interface GovernmentProviderAdapter {

    /**
     * Target government subsystem type (GST, INCOME_TAX, TRACES).
     */
    GovProviderType getProviderType();

    /**
     * Unique alphanumeric adapter identification code (e.g., "MOCK_PROVIDER", "SANDBOX_GSP").
     */
    String getAdapterCode();

    /**
     * Executes a provider-neutral integration request.
     */
    GovIntegrationResult execute(GovIntegrationRequest request);

    /**
     * Performs a liveness/readiness health check against the provider gateway.
     */
    GovProviderHealth checkHealth();

    /**
     * Performs a provider-neutral handshake or connection-level health probe.
     */
    default GovHandshakeResult handshake(GovHandshakeRequest request) {
        GovProviderHealth health = checkHealth();
        if (health != null && health.getStatus() == GovProviderHealth.Status.UP) {
            return GovHandshakeResult.healthy(getProviderType(), getAdapterCode(), health.getLatencyMs(), health.getMessage());
        }
        return GovHandshakeResult.unavailable(getProviderType(), getAdapterCode(),
                health != null ? health.getMessage() : "Provider gateway is unreachable");
    }
}
