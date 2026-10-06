package com.taxoryn.module.compliance.profile.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.client.service.ClientContextService;
import com.taxoryn.module.compliance.profile.dto.ComplianceProfileCompletenessDto;
import com.taxoryn.module.compliance.profile.dto.ComplianceProfileDto;
import com.taxoryn.module.compliance.profile.dto.ComplianceProfileSummaryDto;
import com.taxoryn.module.compliance.profile.dto.GstComplianceConfigDto;
import com.taxoryn.module.compliance.profile.dto.ItrComplianceConfigDto;
import com.taxoryn.module.compliance.profile.dto.OtherComplianceConfigDto;
import com.taxoryn.module.compliance.profile.dto.TdsComplianceConfigDto;
import com.taxoryn.module.compliance.profile.dto.UpdateComplianceProfileRequest;
import com.taxoryn.module.compliance.profile.entity.ComplianceProfileEntity;
import com.taxoryn.module.compliance.profile.model.ComplianceProfileStatus;
import com.taxoryn.module.compliance.profile.repository.ComplianceProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceProfileServiceImpl implements ComplianceProfileService {

    private final ComplianceProfileRepository complianceProfileRepository;
    private final ClientContextService clientContextService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public ComplianceProfileDto getComplianceProfile(UUID clientId) {
        UUID orgId = TenantContext.getTenantId();
        return getComplianceProfile(orgId, clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceProfileDto getComplianceProfile(UUID organizationId, UUID clientId) {
        if (organizationId == null) {
            organizationId = TenantContext.getTenantId();
        }

        // 1. Authoritative check on client existence within organization
        ClientContextSummaryDto clientSummary = clientContextService.requireClientContext(organizationId, clientId);

        // 2. Fetch existing profile or build default unconfigured response
        Optional<ComplianceProfileEntity> entityOpt = complianceProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        if (entityOpt.isPresent()) {
            return mapToDto(entityOpt.get(), clientSummary);
        }

        // Return default unconfigured draft
        return buildDefaultDraftDto(organizationId, clientId, clientSummary);
    }

    @Override
    @Transactional
    public ComplianceProfileDto updateComplianceProfile(UUID clientId, UpdateComplianceProfileRequest request) {
        UUID orgId = TenantContext.getTenantId();
        return updateComplianceProfile(orgId, clientId, request);
    }

    @Override
    @Transactional
    public ComplianceProfileDto updateComplianceProfile(UUID organizationId, UUID clientId, UpdateComplianceProfileRequest request) {
        if (organizationId == null) {
            organizationId = TenantContext.getTenantId();
        }

        // 1. Validate client existence and tenant isolation
        ClientContextSummaryDto clientSummary = clientContextService.requireClientContext(organizationId, clientId);

        // 2. Find or create entity
        Optional<ComplianceProfileEntity> existingOpt = complianceProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId);
        ComplianceProfileEntity entity;
        boolean isNew = false;
        String oldAuditState = null;

        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            oldAuditState = formatAuditSummary(entity);
        } else {
            isNew = true;
            entity = ComplianceProfileEntity.builder()
                    .clientId(clientId)
                    .status(ComplianceProfileStatus.ACTIVE)
                    .build();
            entity.setOrganizationId(organizationId);
        }

        // 3. Apply updates
        applyRequestToEntity(request, entity);

        ComplianceProfileEntity saved = complianceProfileRepository.save(entity);
        String newAuditState = formatAuditSummary(saved);

        // 4. Audit Log
        String action = isNew ? "COMPLIANCE_PROFILE_CREATED" : "COMPLIANCE_PROFILE_UPDATED";
        auditService.logEvent(
                action,
                "COMPLIANCE_PROFILE",
                saved.getId().toString(),
                oldAuditState,
                newAuditState
        );

        log.info("Successfully {} compliance profile id={} for client={} in org={}",
                isNew ? "created" : "updated", saved.getId(), clientId, organizationId);

        return mapToDto(saved, clientSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceProfileSummaryDto> getComplianceProfileSummary(UUID organizationId, UUID clientId) {
        if (organizationId == null) {
            organizationId = TenantContext.getTenantId();
        }
        return complianceProfileRepository.findByOrganizationIdAndClientId(organizationId, clientId)
                .map(this::mapToSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceProfileSummaryDto> getComplianceProfileSummary(UUID clientId) {
        UUID orgId = TenantContext.getTenantId();
        return getComplianceProfileSummary(orgId, clientId);
    }

    private void applyRequestToEntity(UpdateComplianceProfileRequest request, ComplianceProfileEntity entity) {
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        // GST
        if (request.getGstApplicable() != null) {
            entity.setGstApplicable(request.getGstApplicable());
        }
        if (request.getGstRegistrationType() != null) {
            entity.setGstRegistrationType(request.getGstRegistrationType());
        }
        if (request.getGstFilingFrequency() != null) {
            entity.setGstFilingFrequency(request.getGstFilingFrequency());
        }
        if (request.getGstCompositionScheme() != null) {
            entity.setGstCompositionScheme(request.getGstCompositionScheme());
        }
        if (request.getGstEinvoiceApplicable() != null) {
            entity.setGstEinvoiceApplicable(request.getGstEinvoiceApplicable());
        }
        if (request.getGstEwaybillApplicable() != null) {
            entity.setGstEwaybillApplicable(request.getGstEwaybillApplicable());
        }

        // TDS
        if (request.getTdsApplicable() != null) {
            entity.setTdsApplicable(request.getTdsApplicable());
        }
        if (request.getTdsFilingFrequency() != null) {
            entity.setTdsFilingFrequency(request.getTdsFilingFrequency());
        }
        if (request.getTdsDeductorCategory() != null) {
            entity.setTdsDeductorCategory(request.getTdsDeductorCategory());
        }
        if (request.getTdsLowerDeductionCertificate() != null) {
            entity.setTdsLowerDeductionCertificate(request.getTdsLowerDeductionCertificate());
        }

        // ITR
        if (request.getItrApplicable() != null) {
            entity.setItrApplicable(request.getItrApplicable());
        }
        if (request.getItrCategory() != null) {
            entity.setItrCategory(request.getItrCategory());
        }
        if (request.getItrTaxAuditApplicable() != null) {
            entity.setItrTaxAuditApplicable(request.getItrTaxAuditApplicable());
        }
        if (request.getItrTransferPricingApplicable() != null) {
            entity.setItrTransferPricingApplicable(request.getItrTransferPricingApplicable());
        }

        // Other
        if (request.getAdvanceTaxApplicable() != null) {
            entity.setAdvanceTaxApplicable(request.getAdvanceTaxApplicable());
        }
        if (request.getMcaFilingApplicable() != null) {
            entity.setMcaFilingApplicable(request.getMcaFilingApplicable());
        }
        if (request.getProfessionalTaxApplicable() != null) {
            entity.setProfessionalTaxApplicable(request.getProfessionalTaxApplicable());
        }
        if (request.getPfEsiApplicable() != null) {
            entity.setPfEsiApplicable(request.getPfEsiApplicable());
        }

        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes().trim());
        }
    }

    private ComplianceProfileDto mapToDto(ComplianceProfileEntity entity, ClientContextSummaryDto clientSummary) {
        ComplianceProfileCompletenessDto completeness = calculateCompleteness(entity);

        return ComplianceProfileDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .status(entity.getStatus())
                .clientSummary(clientSummary)
                .gstConfig(GstComplianceConfigDto.builder()
                        .applicable(entity.isGstApplicable())
                        .registrationType(entity.getGstRegistrationType())
                        .filingFrequency(entity.getGstFilingFrequency())
                        .compositionScheme(entity.isGstCompositionScheme())
                        .einvoiceApplicable(entity.isGstEinvoiceApplicable())
                        .ewaybillApplicable(entity.isGstEwaybillApplicable())
                        .build())
                .tdsConfig(TdsComplianceConfigDto.builder()
                        .applicable(entity.isTdsApplicable())
                        .filingFrequency(entity.getTdsFilingFrequency())
                        .deductorCategory(entity.getTdsDeductorCategory())
                        .lowerDeductionCertificate(entity.isTdsLowerDeductionCertificate())
                        .build())
                .itrConfig(ItrComplianceConfigDto.builder()
                        .applicable(entity.isItrApplicable())
                        .category(entity.getItrCategory())
                        .taxAuditApplicable(entity.isItrTaxAuditApplicable())
                        .transferPricingApplicable(entity.isItrTransferPricingApplicable())
                        .build())
                .otherComplianceConfig(OtherComplianceConfigDto.builder()
                        .advanceTaxApplicable(entity.isAdvanceTaxApplicable())
                        .mcaFilingApplicable(entity.isMcaFilingApplicable())
                        .professionalTaxApplicable(entity.isProfessionalTaxApplicable())
                        .pfEsiApplicable(entity.isPfEsiApplicable())
                        .build())
                .notes(entity.getNotes())
                .completeness(completeness)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .version(entity.getVersion())
                .build();
    }

    private ComplianceProfileSummaryDto mapToSummaryDto(ComplianceProfileEntity entity) {
        ComplianceProfileCompletenessDto completeness = calculateCompleteness(entity);
        return ComplianceProfileSummaryDto.builder()
                .profileId(entity.getId())
                .clientId(entity.getClientId())
                .status(entity.getStatus())
                .gstApplicable(entity.isGstApplicable())
                .gstRegistrationType(entity.getGstRegistrationType() != null ? entity.getGstRegistrationType().name() : null)
                .gstFilingFrequency(entity.getGstFilingFrequency() != null ? entity.getGstFilingFrequency().name() : null)
                .tdsApplicable(entity.isTdsApplicable())
                .tdsFilingFrequency(entity.getTdsFilingFrequency() != null ? entity.getTdsFilingFrequency().name() : null)
                .itrApplicable(entity.isItrApplicable())
                .itrCategory(entity.getItrCategory() != null ? entity.getItrCategory().name() : null)
                .readinessScore(completeness.getReadinessScore())
                .build();
    }

    private ComplianceProfileDto buildDefaultDraftDto(UUID organizationId, UUID clientId, ClientContextSummaryDto clientSummary) {
        ComplianceProfileCompletenessDto completeness = ComplianceProfileCompletenessDto.builder()
                .configured(false)
                .gstConfigured(false)
                .tdsConfigured(false)
                .itrConfigured(false)
                .readinessScore(0)
                .readinessSummary("UNCONFIGURED")
                .pendingItems(List.of(
                        "Configure GST applicability, registration type and filing frequency",
                        "Configure TDS applicability and deductor category",
                        "Configure ITR applicability and entity category"
                ))
                .build();

        return ComplianceProfileDto.builder()
                .organizationId(organizationId)
                .clientId(clientId)
                .status(ComplianceProfileStatus.DRAFT)
                .clientSummary(clientSummary)
                .gstConfig(GstComplianceConfigDto.builder().applicable(false).build())
                .tdsConfig(TdsComplianceConfigDto.builder().applicable(false).build())
                .itrConfig(ItrComplianceConfigDto.builder().applicable(false).build())
                .otherComplianceConfig(OtherComplianceConfigDto.builder().build())
                .completeness(completeness)
                .build();
    }

    private ComplianceProfileCompletenessDto calculateCompleteness(ComplianceProfileEntity entity) {
        List<String> pendingItems = new ArrayList<>();
        int score = 0;

        // GST evaluation
        boolean gstConfigured = true;
        if (entity.isGstApplicable()) {
            if (entity.getGstRegistrationType() == null) {
                pendingItems.add("GST Registration Type is required when GST is applicable");
                gstConfigured = false;
            }
            if (entity.getGstFilingFrequency() == null) {
                pendingItems.add("GST Filing Frequency is required when GST is applicable");
                gstConfigured = false;
            }
        }
        if (gstConfigured) score += 34;

        // TDS evaluation
        boolean tdsConfigured = true;
        if (entity.isTdsApplicable()) {
            if (entity.getTdsFilingFrequency() == null) {
                pendingItems.add("TDS Filing Frequency is required when TDS is applicable");
                tdsConfigured = false;
            }
            if (entity.getTdsDeductorCategory() == null) {
                pendingItems.add("TDS Deductor Category is required when TDS is applicable");
                tdsConfigured = false;
            }
        }
        if (tdsConfigured) score += 33;

        // ITR evaluation
        boolean itrConfigured = true;
        if (entity.isItrApplicable()) {
            if (entity.getItrCategory() == null) {
                pendingItems.add("ITR Return Category is required when ITR is applicable");
                itrConfigured = false;
            }
        }
        if (itrConfigured) score += 33;

        String readinessSummary = score >= 100 ? "READY" : (score >= 67 ? "PARTIALLY_READY" : "INCOMPLETE");

        return ComplianceProfileCompletenessDto.builder()
                .configured(true)
                .gstConfigured(gstConfigured)
                .tdsConfigured(tdsConfigured)
                .itrConfigured(itrConfigured)
                .readinessScore(score)
                .readinessSummary(readinessSummary)
                .pendingItems(pendingItems)
                .build();
    }

    private String formatAuditSummary(ComplianceProfileEntity entity) {
        return String.format("status=%s, gst=[app=%s, type=%s, freq=%s], tds=[app=%s, freq=%s, cat=%s], itr=[app=%s, cat=%s, audit=%s]",
                entity.getStatus(),
                entity.isGstApplicable(), entity.getGstRegistrationType(), entity.getGstFilingFrequency(),
                entity.isTdsApplicable(), entity.getTdsFilingFrequency(), entity.getTdsDeductorCategory(),
                entity.isItrApplicable(), entity.getItrCategory(), entity.isItrTaxAuditApplicable());
    }
}
