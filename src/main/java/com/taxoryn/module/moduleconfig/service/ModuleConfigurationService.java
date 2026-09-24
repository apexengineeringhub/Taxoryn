package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.module.moduleconfig.dto.OrganizationModuleDto;
import com.taxoryn.module.moduleconfig.dto.ProductModuleDto;
import com.taxoryn.module.moduleconfig.model.ProductModuleCode;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Central Service for resolving and updating organization product module configurations.
 */
public interface ModuleConfigurationService {

    /**
     * Checks if a module is enabled for the current authenticated organization.
     */
    boolean isModuleEnabled(ProductModuleCode moduleCode);

    /**
     * Checks if a module is enabled for a specific organization ID.
     */
    boolean isModuleEnabled(UUID organizationId, ProductModuleCode moduleCode);

    /**
     * Returns the set of all enabled module codes for an organization.
     */
    Set<ProductModuleCode> getEnabledModules(UUID organizationId);

    /**
     * Returns the full list of all product modules with their resolved enabled/disabled status for an organization.
     */
    List<OrganizationModuleDto> getOrganizationModules(UUID organizationId);

    /**
     * Returns the configuration of a single product module for an organization.
     */
    OrganizationModuleDto getOrganizationModule(UUID organizationId, ProductModuleCode moduleCode);

    /**
     * Enables or disables a product module for an organization, recording an audit event.
     */
    OrganizationModuleDto updateModuleStatus(UUID organizationId, ProductModuleCode moduleCode, boolean enabled);

    /**
     * Retrieves the global product module catalog master list.
     */
    List<ProductModuleDto> getAllProductModules();
}
