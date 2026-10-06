package com.taxoryn.module.compliance.applicability.provider;

import com.taxoryn.module.compliance.applicability.model.ComplianceFactContext;

import java.util.UUID;

/**
 * Public provider contract for resolving client compliance and statutory facts
 * into an evaluation context.
 */
public interface ComplianceFactProvider {

    /**
     * Resolves consolidated compliance facts for a given client within tenant context.
     *
     * @param clientId client UUID
     * @return populated ComplianceFactContext
     */
    ComplianceFactContext resolveFacts(UUID clientId);

    /**
     * Resolves consolidated compliance facts for a given client and organization.
     *
     * @param organizationId organization/tenant UUID
     * @param clientId       client UUID
     * @return populated ComplianceFactContext
     */
    ComplianceFactContext resolveFacts(UUID organizationId, UUID clientId);
}
