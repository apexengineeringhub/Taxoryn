package com.taxoryn.module.gov.service;

import com.taxoryn.module.gov.dto.GovIntegrationRequest;
import com.taxoryn.module.gov.dto.GovIntegrationResult;
import com.taxoryn.module.gov.dto.GovOperationDto;
import com.taxoryn.module.gov.model.GovOperationStatus;
import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.spi.GovernmentProviderAdapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core application service interface for the Government Integration bounded context.
 */
public interface GovernmentIntegrationService {

    /**
     * Executes a provider-neutral government integration operation with idempotency and lifecycle tracking.
     */
    GovIntegrationResult executeOperation(GovIntegrationRequest request);

    /**
     * Retrieves an operation by its primary key ID within the active tenant scope.
     */
    GovOperationDto getOperation(UUID operationId);

    /**
     * Retrieves an operation by its correlation trace ID within the active tenant scope.
     */
    GovOperationDto getOperationByCorrelationId(String correlationId);

    /**
     * Retrieves an operation by its idempotency key within the active tenant scope.
     */
    GovOperationDto getOperationByIdempotencyKey(String idempotencyKey);

    /**
     * Lists operations by status within the active tenant scope.
     */
    List<GovOperationDto> getOperationsByStatus(GovOperationStatus status);

    /**
     * Lists operations associated with a specific business entity within the active tenant scope.
     */
    List<GovOperationDto> getOperationsByBusinessEntity(String businessEntityType, UUID businessEntityId);

    /**
     * Dynamically registers a provider adapter SPI implementation.
     */
    void registerAdapter(GovernmentProviderAdapter adapter);

    /**
     * Returns list of registered adapter codes.
     */
    List<String> getAvailableAdapters();

    /**
     * Looks up an adapter for a given provider type.
     */
    Optional<GovernmentProviderAdapter> getAdapter(GovProviderType providerType);
}
