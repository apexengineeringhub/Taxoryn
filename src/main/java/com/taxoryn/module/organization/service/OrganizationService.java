package com.taxoryn.module.organization.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.organization.dto.CreateOrganizationRequest;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.dto.OrganizationSettingsDto;
import com.taxoryn.module.organization.dto.UpdateOrganizationRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationSettingsRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationStatusRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;

import java.util.UUID;

public interface OrganizationService {

    OrganizationDto createOrganization(CreateOrganizationRequest request);

    OrganizationDto getOrganizationById(UUID organizationId);

    OrganizationDto getCurrentOrganization();

    PagedResponse<OrganizationDto> getOrganizations(PageRequestDto pageRequest);

    OrganizationDto updateOrganization(UUID organizationId, UpdateOrganizationRequest request);

    OrganizationDto updateCurrentOrganization(UpdateOrganizationRequest request);

    OrganizationDto updateOrganizationStatus(UUID organizationId, UpdateOrganizationStatusRequest request);

    OrganizationSettingsDto getOrganizationSettings(UUID organizationId);

    OrganizationSettingsDto getCurrentOrganizationSettings();

    OrganizationSettingsDto updateOrganizationSettings(UUID organizationId, UpdateOrganizationSettingsRequest request);

    OrganizationSettingsDto updateCurrentOrganizationSettings(UpdateOrganizationSettingsRequest request);

    OrganizationEntity getOrganizationEntityById(UUID organizationId);
}
