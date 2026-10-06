package com.taxoryn.module.client.businesscontext.provider;

import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;

import java.util.UUID;

/**
 * Lightweight provider contract for contextual read-model resolution across bounded contexts.
 * Providers strictly consume application service contracts and return DTOs/value objects (no JPA entities).
 *
 * @param <T> the contextual DTO type produced by this provider
 */
public interface BusinessContextProvider<T> {

    /**
     * Unique identifier key for this context provider.
     */
    String getProviderKey();

    /**
     * Resolves the context component for the given request, tenant, and validated client.
     *
     * @param request the business context request containing identifiers
     * @param organizationId the verified tenant ID
     * @param resolvedClientId the verified client ID
     * @return contextual DTO or null if not applicable / not requested
     */
    T resolve(BusinessContextRequest request, UUID organizationId, UUID resolvedClientId);
}
