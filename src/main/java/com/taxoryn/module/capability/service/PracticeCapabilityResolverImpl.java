package com.taxoryn.module.capability.service;

import com.taxoryn.core.exception.ForbiddenException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.capability.dto.OrganizationCapabilitiesDto;
import com.taxoryn.module.capability.model.ProductCapability;
import com.taxoryn.module.organization.entity.OrganizationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeCapabilityResolverImpl implements PracticeCapabilityResolver {

    private final ProductCapabilityService productCapabilityService;

    @Override
    public boolean hasCapability(ProductCapability capability) {
        return productCapabilityService.isCapabilityEnabled(capability);
    }

    @Override
    public boolean hasCapability(UUID organizationId, ProductCapability capability) {
        return productCapabilityService.isCapabilityEnabled(organizationId, capability);
    }

    @Override
    public void requireCapability(ProductCapability capability, String forbiddenMessage) {
        if (!hasCapability(capability)) {
            throw new ForbiddenException(forbiddenMessage);
        }
    }

    @Override
    public void requireCapability(UUID organizationId, ProductCapability capability, String forbiddenMessage) {
        if (!hasCapability(organizationId, capability)) {
            throw new ForbiddenException(forbiddenMessage);
        }
    }

    @Override
    public boolean isSoloPractice() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            return false;
        }
        return isSoloPractice(organizationId);
    }

    @Override
    public boolean isSoloPractice(UUID organizationId) {
        if (organizationId == null) {
            return false;
        }
        OrganizationCapabilitiesDto caps = productCapabilityService.getCapabilitiesByOrganizationId(organizationId);
        return caps.getOrganizationType() == OrganizationType.SOLO || caps.getOrganizationType() == OrganizationType.SOLO_PRACTITIONER;
    }
}
