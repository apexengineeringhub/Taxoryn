package com.taxoryn.module.capability.service;

import com.taxoryn.module.capability.dto.OrganizationCapabilitiesDto;
import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.organization.entity.OrganizationType;

import java.util.UUID;

/**
 * Service for resolving organization product capabilities, feature profiles,
 * and experience defaults.
 */
public interface ProductCapabilityService {

    /**
     * Resolves the complete capability and experience configuration for the authenticated tenant.
     *
     * @return OrganizationCapabilitiesDto
     */
    OrganizationCapabilitiesDto getCurrentOrganizationCapabilities();

    /**
     * Resolves the capability configuration for a specific organization.
     *
     * @param organizationId Organization UUID
     * @return OrganizationCapabilitiesDto
     */
    OrganizationCapabilitiesDto getCapabilitiesByOrganizationId(UUID organizationId);

    /**
     * Checks if a specific capability is enabled for the authenticated tenant.
     *
     * @param capability ProductCapability to test
     * @return true if enabled, false otherwise
     */
    boolean isCapabilityEnabled(ProductCapability capability);

    /**
     * Checks if a specific capability is enabled for a given organization.
     *
     * @param organizationId Organization UUID
     * @param capability ProductCapability to test
     * @return true if enabled, false otherwise
     */
    boolean isCapabilityEnabled(UUID organizationId, ProductCapability capability);
}
