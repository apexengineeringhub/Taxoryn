package com.taxoryn.module.gov.spi;

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
}
