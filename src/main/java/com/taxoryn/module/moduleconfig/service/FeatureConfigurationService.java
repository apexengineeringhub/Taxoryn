package com.taxoryn.module.moduleconfig.service;

import com.taxoryn.module.moduleconfig.dto.OrganizationFeatureDto;
import com.taxoryn.module.moduleconfig.dto.ProductFeatureDto;

import java.util.List;
import java.util.UUID;

public interface FeatureConfigurationService {

    boolean isFeatureEnabled(String moduleCode, String featureCode);

    boolean isFeatureEnabled(UUID organizationId, String moduleCode, String featureCode);

    void verifyFeatureAccess(UUID organizationId, String moduleCode, String featureCode);

    List<OrganizationFeatureDto> getOrganizationFeatures(UUID organizationId, String moduleCode);

    List<OrganizationFeatureDto> getAllOrganizationFeatures(UUID organizationId);

    OrganizationFeatureDto updateFeatureStatus(UUID organizationId, String moduleCode, String featureCode, boolean enabled);

    List<ProductFeatureDto> getCatalogFeatures(String moduleCode);
}
