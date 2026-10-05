package com.taxoryn.module.client.service;

import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.client.dto.ClientLifecycleSummaryDto;
import com.taxoryn.module.client.dto.UpdateClientStatusRequest;
import com.taxoryn.module.client.entity.ClientEntity.ClientStatus;

import java.util.Set;
import java.util.UUID;

/**
 * Dedicated application-level service for Client Lifecycle and Status governance.
 * Enforces explicit transition rules, tenant safety, status metadata tracking,
 * session governance on archive, and audit trail emission.
 */
public interface ClientLifecycleService {

    /**
     * Executes a validated client lifecycle transition.
     *
     * @param clientId the target client ID
     * @param request the transition request payload containing target status and optional reason
     * @return updated client DTO
     */
    ClientDto transitionStatus(UUID clientId, UpdateClientStatusRequest request);

    /**
     * Retrieves the authoritative lifecycle state summary for a client.
     *
     * @param clientId the target client ID
     * @return lifecycle summary DTO
     */
    ClientLifecycleSummaryDto getLifecycleSummary(UUID clientId);

    /**
     * Checks if a transition from currentStatus to targetStatus is valid.
     *
     * @param currentStatus current client lifecycle status
     * @param targetStatus target client lifecycle status
     * @return true if permitted, false otherwise
     */
    boolean isTransitionAllowed(ClientStatus currentStatus, ClientStatus targetStatus);

    /**
     * Returns the set of valid next lifecycle states reachable from the given status.
     *
     * @param currentStatus current client lifecycle status
     * @return set of permitted target statuses
     */
    Set<ClientStatus> getAllowedTransitions(ClientStatus currentStatus);
}
