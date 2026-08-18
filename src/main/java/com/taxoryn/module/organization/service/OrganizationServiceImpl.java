package com.taxoryn.module.organization.service;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.core.exception.DuplicateResourceException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.TenantAccessDeniedException;
import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.organization.dto.CreateOrganizationRequest;
import com.taxoryn.module.organization.dto.OrganizationDto;
import com.taxoryn.module.organization.dto.OrganizationSettingsDto;
import com.taxoryn.module.organization.dto.UpdateOrganizationRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationSettingsRequest;
import com.taxoryn.module.organization.dto.UpdateOrganizationStatusRequest;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationSettingsEntity;
import com.taxoryn.module.organization.mapper.OrganizationMapper;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import com.taxoryn.module.organization.repository.OrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final OrganizationMapper organizationMapper;

    @Override
    @Transactional
    public OrganizationDto createOrganization(CreateOrganizationRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (organizationRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Organization", "email", email);
        }

        OrganizationEntity organization = OrganizationEntity.builder()
                .name(request.getName().trim())
                .legalName(request.getLegalName())
                .tradeName(request.getTradeName())
                .email(email)
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .pincode(request.getPincode())
                .pan(request.getPan())
                .gstin(request.getGstin())
                .taxRegistrationNumber(request.getTaxRegistrationNumber())
                .subscriptionPlan(request.getSubscriptionPlan() != null ? request.getSubscriptionPlan() : OrganizationEntity.SubscriptionPlan.STARTER)
                .status(OrganizationEntity.OrganizationStatus.ACTIVE)
                .build();

        OrganizationEntity saved = organizationRepository.save(organization);

        // Auto-provision default settings for new organization
        OrganizationSettingsEntity defaultSettings = OrganizationSettingsEntity.createDefault(saved.getId());
        OrganizationSettingsEntity savedSettings = settingsRepository.save(defaultSettings);
        saved.setSettings(savedSettings);

        log.info("Created organization: id={}, name={}", saved.getId(), saved.getName());
        return organizationMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDto getOrganizationById(UUID organizationId) {
        validateTenantAccess(organizationId);
        OrganizationEntity entity = getOrganizationEntityById(organizationId);
        return organizationMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDto getCurrentOrganization() {
        UUID currentTenantId = SecurityUtils.getCurrentOrganizationId();
        OrganizationEntity entity = getOrganizationEntityById(currentTenantId);
        return organizationMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrganizationDto> getOrganizations(PageRequestDto pageRequest) {
        Page<OrganizationEntity> page = organizationRepository.findAll(pageRequest.toPageable());
        return PagedResponse.of(page, organizationMapper::toDto);
    }

    @Override
    @Transactional
    public OrganizationDto updateOrganization(UUID organizationId, UpdateOrganizationRequest request) {
        validateTenantAccess(organizationId);
        OrganizationEntity entity = getOrganizationEntityById(organizationId);

        entity.setName(request.getName().trim());
        if (request.getLegalName() != null) entity.setLegalName(request.getLegalName().trim());
        if (request.getTradeName() != null) entity.setTradeName(request.getTradeName().trim());
        if (request.getPhone() != null) entity.setPhone(request.getPhone());
        if (request.getAddress() != null) entity.setAddress(request.getAddress());
        if (request.getCity() != null) entity.setCity(request.getCity());
        if (request.getState() != null) entity.setState(request.getState());
        if (request.getCountry() != null) entity.setCountry(request.getCountry());
        if (request.getPincode() != null) entity.setPincode(request.getPincode());
        if (request.getPan() != null) entity.setPan(request.getPan());
        if (request.getGstin() != null) entity.setGstin(request.getGstin());
        if (request.getTaxRegistrationNumber() != null) entity.setTaxRegistrationNumber(request.getTaxRegistrationNumber());

        OrganizationEntity saved = organizationRepository.save(entity);
        log.info("Updated organization: id={}", saved.getId());
        return organizationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public OrganizationDto updateCurrentOrganization(UpdateOrganizationRequest request) {
        UUID currentTenantId = SecurityUtils.getCurrentOrganizationId();
        return updateOrganization(currentTenantId, request);
    }

    @Override
    @Transactional
    public OrganizationDto updateOrganizationStatus(UUID organizationId, UpdateOrganizationStatusRequest request) {
        validateTenantAccess(organizationId);
        OrganizationEntity entity = getOrganizationEntityById(organizationId);

        entity.setStatus(request.getStatus());
        OrganizationEntity saved = organizationRepository.save(entity);
        log.info("Updated organization status: id={}, newStatus={}, reason={}", organizationId, request.getStatus(), request.getReason());
        return organizationMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationSettingsDto getOrganizationSettings(UUID organizationId) {
        validateTenantAccess(organizationId);
        OrganizationSettingsEntity settings = settingsRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> settingsRepository.save(OrganizationSettingsEntity.createDefault(organizationId)));
        return organizationMapper.toSettingsDto(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationSettingsDto getCurrentOrganizationSettings() {
        UUID currentTenantId = SecurityUtils.getCurrentOrganizationId();
        return getOrganizationSettings(currentTenantId);
    }

    @Override
    @Transactional
    public OrganizationSettingsDto updateOrganizationSettings(UUID organizationId, UpdateOrganizationSettingsRequest request) {
        validateTenantAccess(organizationId);
        OrganizationSettingsEntity settings = settingsRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> OrganizationSettingsEntity.createDefault(organizationId));

        settings.setTimezone(request.getTimezone());
        settings.setDateFormat(request.getDateFormat());
        settings.setCurrency(request.getCurrency());
        settings.setFinancialYearStartMonth(request.getFinancialYearStartMonth());

        if (request.getEnableEmailNotifications() != null) {
            settings.setEnableEmailNotifications(request.getEnableEmailNotifications());
        }
        if (request.getEnableSmsNotifications() != null) {
            settings.setEnableSmsNotifications(request.getEnableSmsNotifications());
        }
        if (request.getEnableWhatsappNotifications() != null) {
            settings.setEnableWhatsappNotifications(request.getEnableWhatsappNotifications());
        }
        if (request.getInvoicePrefix() != null) {
            settings.setInvoicePrefix(request.getInvoicePrefix().trim());
        }
        if (request.getAutoRemindersEnabled() != null) {
            settings.setAutoRemindersEnabled(request.getAutoRemindersEnabled());
        }

        OrganizationSettingsEntity saved = settingsRepository.save(settings);
        log.info("Updated settings for organization: id={}", organizationId);
        return organizationMapper.toSettingsDto(saved);
    }

    @Override
    @Transactional
    public OrganizationSettingsDto updateCurrentOrganizationSettings(UpdateOrganizationSettingsRequest request) {
        UUID currentTenantId = SecurityUtils.getCurrentOrganizationId();
        return updateOrganizationSettings(currentTenantId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationEntity getOrganizationEntityById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
    }

    private void validateTenantAccess(UUID requestedOrganizationId) {
        if (SecurityUtils.hasRole("SUPER_ADMIN")) {
            return;
        }

        UUID currentTenantId = SecurityUtils.getCurrentOrganizationId();
        if (currentTenantId == null || !currentTenantId.equals(requestedOrganizationId)) {
            throw new TenantAccessDeniedException("Cross-tenant access violation: Action denied for organization " + requestedOrganizationId);
        }
    }
}
