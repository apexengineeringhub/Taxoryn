package com.taxoryn.module.billing.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.PracticeSecurityScope;
import com.taxoryn.core.security.PracticeSecurityScopeEvaluator;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.billing.dto.BillingProfileDto;
import com.taxoryn.module.billing.dto.CreateBillingProfileRequest;
import com.taxoryn.module.billing.dto.UpdateBillingProfileRequest;
import com.taxoryn.module.billing.entity.BillingProfileEntity;
import com.taxoryn.module.billing.repository.BillingProfileRepository;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.engagement.entity.EngagementEntity;
import com.taxoryn.module.engagement.repository.EngagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingProfileServiceImpl implements BillingProfileService {

    private final BillingProfileRepository billingProfileRepository;
    private final ClientRepository clientRepository;
    private final EngagementRepository engagementRepository;
    private final PracticeSecurityScopeEvaluator securityScopeEvaluator;
    private final AuditService auditService;

    @Override
    @Transactional
    public BillingProfileDto createBillingProfile(CreateBillingProfileRequest request) {
        UUID organizationId = resolveOrganizationId();

        ClientEntity client = clientRepository.findByIdAndOrganizationId(request.getClientId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", request.getClientId()));

        validateAccess(client.getId(), client.getLocationId());

        if (request.getEngagementId() != null) {
            engagementRepository.findByIdAndOrganizationId(request.getEngagementId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Engagement", "id", request.getEngagementId()));
        }

        BillingProfileEntity profile = BillingProfileEntity.builder()
                .clientId(client.getId())
                .engagementId(request.getEngagementId())
                .billingFrequency(StringUtils.hasText(request.getBillingFrequency()) ? request.getBillingFrequency().trim() : "MONTHLY")
                .currency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim() : "INR")
                .defaultRate(request.getDefaultRate())
                .taxApplicable(request.getTaxApplicable() != null ? request.getTaxApplicable() : true)
                .active(request.getActive() != null ? request.getActive() : true)
                .notes(request.getNotes())
                .build();
        profile.setOrganizationId(organizationId);

        profile = billingProfileRepository.save(profile);
        log.info("Created Billing Profile: id={} for tenant={}", profile.getId(), organizationId);

        auditService.logEvent("BILLING_PROFILE_CREATED", "BILLING_PROFILE", profile.getId().toString(), null, profile);

        return enrichDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public BillingProfileDto getBillingProfileById(UUID id) {
        UUID organizationId = resolveOrganizationId();
        BillingProfileEntity profile = billingProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("BillingProfile", "id", id));

        validateAccess(profile.getClientId(), null);

        return enrichDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingProfileDto> getBillingProfilesByClientId(UUID clientId) {
        UUID organizationId = resolveOrganizationId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        validateAccess(client.getId(), client.getLocationId());

        List<BillingProfileEntity> list = billingProfileRepository.findAllByOrganizationIdAndClientIdOrderByCreatedAtDesc(organizationId, clientId);
        return enrichDtoList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingProfileDto> getAllBillingProfiles() {
        UUID organizationId = resolveOrganizationId();
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();

        List<BillingProfileEntity> list = billingProfileRepository.findAllByOrganizationId(organizationId).stream()
                .filter(p -> {
                    if (scope.isFirmAdmin()) return true;
                    Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
                    return accessibleClients == null || accessibleClients.contains(p.getClientId());
                })
                .collect(Collectors.toList());

        return enrichDtoList(list);
    }

    @Override
    @Transactional
    public BillingProfileDto updateBillingProfile(UUID id, UpdateBillingProfileRequest request) {
        UUID organizationId = resolveOrganizationId();
        BillingProfileEntity profile = billingProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("BillingProfile", "id", id));

        validateAccess(profile.getClientId(), null);

        if (request.getEngagementId() != null) {
            profile.setEngagementId(request.getEngagementId());
        }
        if (StringUtils.hasText(request.getBillingFrequency())) {
            profile.setBillingFrequency(request.getBillingFrequency().trim());
        }
        if (StringUtils.hasText(request.getCurrency())) {
            profile.setCurrency(request.getCurrency().trim());
        }
        if (request.getDefaultRate() != null) {
            profile.setDefaultRate(request.getDefaultRate());
        }
        if (request.getTaxApplicable() != null) {
            profile.setTaxApplicable(request.getTaxApplicable());
        }
        if (request.getActive() != null) {
            profile.setActive(request.getActive());
        }
        if (request.getNotes() != null) {
            profile.setNotes(request.getNotes());
        }

        profile = billingProfileRepository.save(profile);
        log.info("Updated Billing Profile: id={}", profile.getId());

        auditService.logEvent("BILLING_PROFILE_UPDATED", "BILLING_PROFILE", profile.getId().toString(), null, profile);

        return enrichDto(profile);
    }

    @Override
    @Transactional
    public void deleteBillingProfile(UUID id) {
        UUID organizationId = resolveOrganizationId();
        BillingProfileEntity profile = billingProfileRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("BillingProfile", "id", id));

        validateAccess(profile.getClientId(), null);

        billingProfileRepository.delete(profile);
        log.info("Deleted Billing Profile: id={}", id);

        auditService.logEvent("BILLING_PROFILE_DELETED", "BILLING_PROFILE", id.toString(), null, null);
    }

    // --- Helper Methods ---

    private UUID resolveOrganizationId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        return SecurityUtils.getCurrentOrganizationId();
    }

    private void validateAccess(UUID clientId, UUID locationId) {
        PracticeSecurityScope scope = securityScopeEvaluator.evaluateCurrentScope();
        if (scope.isFirmAdmin()) {
            return;
        }

        if (locationId != null) {
            Set<UUID> accessibleLocs = scope.getAccessibleLocationIds();
            if (accessibleLocs != null && !accessibleLocs.isEmpty() && !accessibleLocs.contains(locationId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this location");
            }
        }

        if (clientId != null) {
            Set<UUID> accessibleClients = securityScopeEvaluator.getAccessibleClientIds(scope);
            if (accessibleClients != null && !accessibleClients.contains(clientId)) {
                throw new AccessDeniedException("Access denied: You do not have permission for this client");
            }
        }
    }

    private BillingProfileDto enrichDto(BillingProfileEntity entity) {
        UUID orgId = entity.getOrganizationId();
        String clientName = null;
        if (entity.getClientId() != null) {
            clientName = clientRepository.findByIdAndOrganizationId(entity.getClientId(), orgId)
                    .map(ClientEntity::getDisplayName)
                    .orElse(null);
        }

        String engagementName = null;
        if (entity.getEngagementId() != null) {
            engagementName = engagementRepository.findByIdAndOrganizationId(entity.getEngagementId(), orgId)
                    .map(EngagementEntity::getName)
                    .orElse(null);
        }

        return BillingProfileDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(clientName)
                .engagementId(entity.getEngagementId())
                .engagementName(engagementName)
                .billingFrequency(entity.getBillingFrequency())
                .currency(entity.getCurrency())
                .defaultRate(entity.getDefaultRate())
                .taxApplicable(entity.getTaxApplicable())
                .active(entity.getActive())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private List<BillingProfileDto> enrichDtoList(List<BillingProfileEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        UUID orgId = entities.get(0).getOrganizationId();
        Map<UUID, String> clientNames = clientRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(ClientEntity::getId, ClientEntity::getDisplayName, (a, b) -> a));
        Map<UUID, String> engagementNames = engagementRepository.findAllByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(EngagementEntity::getId, EngagementEntity::getName, (a, b) -> a));

        return entities.stream().map(e -> BillingProfileDto.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .clientId(e.getClientId())
                .clientName(e.getClientId() != null ? clientNames.get(e.getClientId()) : null)
                .engagementId(e.getEngagementId())
                .engagementName(e.getEngagementId() != null ? engagementNames.get(e.getEngagementId()) : null)
                .billingFrequency(e.getBillingFrequency())
                .currency(e.getCurrency())
                .defaultRate(e.getDefaultRate())
                .taxApplicable(e.getTaxApplicable())
                .active(e.getActive())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build()).collect(Collectors.toList());
    }
}
