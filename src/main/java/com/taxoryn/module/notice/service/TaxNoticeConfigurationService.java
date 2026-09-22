package com.taxoryn.module.notice.service;

import com.taxoryn.module.notice.dto.TaxNoticeConfigDto;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeConfigRequest;
import com.taxoryn.module.organization.entity.OrganizationType;

import java.util.UUID;

public interface TaxNoticeConfigurationService {

    /**
     * Resolves the effective configuration for the current authenticated organization.
     * Follows hierarchy: Explicit Org Setting -> Persona Default -> Safe Platform Default.
     */
    TaxNoticeConfigDto getEffectiveConfiguration();

    /**
     * Resolves the effective configuration for a given organization ID.
     */
    TaxNoticeConfigDto getEffectiveConfiguration(UUID organizationId);

    /**
     * Updates/persists explicit configuration for the current authenticated organization.
     */
    TaxNoticeConfigDto updateConfiguration(UpdateTaxNoticeConfigRequest request);

    /**
     * Updates/persists explicit configuration for a given organization ID.
     */
    TaxNoticeConfigDto updateConfiguration(UUID organizationId, UpdateTaxNoticeConfigRequest request);

    /**
     * Retrieves the standard defaults for a specific OrganizationType persona.
     */
    TaxNoticeConfigDto getPersonaDefaults(OrganizationType organizationType);

    /**
     * Resets organization configuration back to its persona default.
     */
    TaxNoticeConfigDto resetToPersonaDefaults(UUID organizationId);
}
