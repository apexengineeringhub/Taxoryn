package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientContextSummaryDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight application-level client context contract.
 * Allows other bounded contexts to resolve client metadata, status, and tenant ownership
 * without accessing ClientRepository or ClientEntity directly.
 */
public interface ClientContextService {

    /**
     * Finds client context by explicit organization ID and client ID.
     */
    Optional<ClientContextSummaryDto> findClientContext(UUID organizationId, UUID clientId);

    /**
     * Finds client context using the current authenticated tenant context.
     */
    Optional<ClientContextSummaryDto> findClientContext(UUID clientId);

    /**
     * Resolves client context or throws {@link com.taxoryn.core.exception.ResourceNotFoundException}.
     */
    ClientContextSummaryDto requireClientContext(UUID organizationId, UUID clientId);

    /**
     * Resolves client context in current tenant or throws {@link com.taxoryn.core.exception.ResourceNotFoundException}.
     */
    ClientContextSummaryDto requireClientContext(UUID clientId);

    /**
     * Checks if a client exists within the specified organization.
     */
    boolean exists(UUID organizationId, UUID clientId);

    /**
     * Checks if a client exists and is in ACTIVE status within the specified organization.
     */
    boolean isActive(UUID organizationId, UUID clientId);
}
