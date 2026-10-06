package com.taxoryn.module.gov.auth.spi;

import com.taxoryn.module.gov.auth.dto.GovAuthSessionDto;
import com.taxoryn.module.gov.auth.entity.GovAuthSessionEntity;
import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gov.dto.GovConnectionDto;
import com.taxoryn.module.gov.model.GovProviderType;

import java.util.Map;
import java.util.Set;

/**
 * Service Provider Interface (SPI) for provider-neutral government authentication gateways.
 */
public interface GovernmentAuthenticationProvider {

    /**
     * Target government subsystem type (GST, INCOME_TAX, TDS).
     */
    GovProviderType getProviderType();

    /**
     * Set of authentication methods supported by this provider adapter.
     */
    Set<GovAuthMethod> getSupportedMethods();

    /**
     * Initiates authentication with the provider subsystem.
     */
    GovAuthSessionDto startAuthentication(
            GovConnectionDto connection,
            GovAuthMethod method,
            String correlationId,
            Map<String, Object> options
    );

    /**
     * Checks or refreshes status of an existing authentication session with the provider.
     */
    GovAuthSessionDto checkAuthenticationStatus(
            GovConnectionDto connection,
            GovAuthSessionEntity session,
            Map<String, Object> options
    );

    /**
     * Continues or completes an interactive authorization flow (e.g., following user action / challenge response).
     */
    GovAuthSessionDto continueAuthorization(
            GovConnectionDto connection,
            GovAuthSessionEntity session,
            String actionReference,
            Map<String, Object> options
    );

    /**
     * Revokes or logs out the active session with the provider gateway.
     */
    GovAuthSessionDto revokeAuthentication(
            GovConnectionDto connection,
            GovAuthSessionEntity session
    );
}
