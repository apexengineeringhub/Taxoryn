package com.taxoryn.module.gov.outbox.handler;

import com.taxoryn.module.gov.outbox.entity.GovOutboxEventEntity;

/**
 * Service Provider Interface for handling specific outbox event types asynchronously.
 */
public interface GovOutboxHandler {

    /**
     * Determines whether this handler supports processing the given event type.
     */
    boolean supports(String eventType);

    /**
     * Executes the outbox event logic.
     * Throws an exception on transient or terminal failure to trigger processor retry/failure logic.
     */
    void handle(GovOutboxEventEntity event);
}
