package com.taxoryn.module.compliance.profile.service;

import com.taxoryn.module.compliance.profile.dto.ComplianceProfileDto;
import com.taxoryn.module.compliance.profile.dto.ComplianceProfileSummaryDto;
import com.taxoryn.module.compliance.profile.dto.UpdateComplianceProfileRequest;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Client Compliance Profiles (Phase 29.1).
 * Establishes compliance facts and configurations without generating obligations or tasks.
 */
public interface ComplianceProfileService {

    /**
     * Retrieves the compliance profile for a client within the current tenant context.
     * If no profile exists yet, returns a default unconfigured draft profile.
     */
    ComplianceProfileDto getComplianceProfile(UUID clientId);

    /**
     * Retrieves the compliance profile with explicit organization ID.
     */
    ComplianceProfileDto getComplianceProfile(UUID organizationId, UUID clientId);

    /**
     * Creates or updates the compliance profile for a client within the current tenant context.
     */
    ComplianceProfileDto updateComplianceProfile(UUID clientId, UpdateComplianceProfileRequest request);

    /**
     * Creates or updates the compliance profile with explicit organization ID.
     */
    ComplianceProfileDto updateComplianceProfile(UUID organizationId, UUID clientId, UpdateComplianceProfileRequest request);

    /**
     * Retrieves lightweight compliance profile summary for cross-domain read models.
     */
    Optional<ComplianceProfileSummaryDto> getComplianceProfileSummary(UUID organizationId, UUID clientId);

    /**
     * Retrieves lightweight compliance profile summary within the current tenant context.
     */
    Optional<ComplianceProfileSummaryDto> getComplianceProfileSummary(UUID clientId);
}
