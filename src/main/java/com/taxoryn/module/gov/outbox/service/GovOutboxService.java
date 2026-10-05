package com.taxoryn.module.gov.outbox.service;

import com.taxoryn.module.gov.model.GovProviderType;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEnqueueRequest;
import com.taxoryn.module.gov.outbox.dto.GovOutboxEventDto;
import com.taxoryn.module.gov.outbox.model.GovOutboxStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for transactional enqueueing, querying, and managing government outbox events.
 */
public interface GovOutboxService {

    /**
     * Enqueues an event into the transactional outbox table within the current database transaction.
     */
    GovOutboxEventDto enqueue(GovOutboxEnqueueRequest request);

    /**
     * Convenience method to schedule an asynchronous operation dispatch outbox event.
     */
    GovOutboxEventDto scheduleOperationDispatch(UUID operationId, GovProviderType providerType, String correlationId, String payloadJson);

    /**
     * Retrieves an outbox event by ID for the current tenant.
     */
    Optional<GovOutboxEventDto> findById(UUID id);

    /**
     * Retrieves all outbox events with a specific status for the current tenant.
     */
    List<GovOutboxEventDto> findByStatus(GovOutboxStatus status);

    /**
     * Retrieves all outbox events for a specific operation for the current tenant.
     */
    List<GovOutboxEventDto> findByOperationId(UUID operationId);

    /**
     * Cancels an enqueued or pending outbox event for the current tenant.
     */
    GovOutboxEventDto cancel(UUID id);

    /**
     * Returns count of pending events for the current tenant.
     */
    long countPending();
}
