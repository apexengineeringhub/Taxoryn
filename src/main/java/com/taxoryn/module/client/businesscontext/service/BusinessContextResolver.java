package com.taxoryn.module.client.businesscontext.service;

import com.taxoryn.module.client.businesscontext.dto.BusinessContextDto;
import com.taxoryn.module.client.businesscontext.dto.BusinessContextRequest;

import java.util.UUID;

/**
 * Reusable, lightweight, stateless Business Context Resolver.
 * Orchestrates domain-specific context providers to resolve composite context without
 * directly coupling bounded contexts together or performing cross-module repository access.
 */
public interface BusinessContextResolver {

    /**
     * Resolves composite Business Context according to provided identifiers.
     *
     * @param request the business context request
     * @return unified BusinessContextDto
     */
    BusinessContextDto resolveContext(BusinessContextRequest request);

    /**
     * Convenience method to resolve Client-centric context.
     *
     * @param clientId the client ID
     * @return BusinessContextDto
     */
    BusinessContextDto resolveClientContext(UUID clientId);

    /**
     * Convenience method to resolve Client + Service context.
     *
     * @param clientId the client ID
     * @param serviceRelationshipId the service relationship ID
     * @return BusinessContextDto
     */
    BusinessContextDto resolveServiceContext(UUID clientId, UUID serviceRelationshipId);

    /**
     * Convenience method to resolve Client + Engagement context.
     *
     * @param clientId the client ID
     * @param engagementId the engagement ID
     * @return BusinessContextDto
     */
    BusinessContextDto resolveEngagementContext(UUID clientId, UUID engagementId);

    /**
     * Convenience method to resolve Client + Work / Task context.
     *
     * @param clientId the client ID
     * @param taskId the task ID
     * @return BusinessContextDto
     */
    BusinessContextDto resolveWorkContext(UUID clientId, UUID taskId);
}
