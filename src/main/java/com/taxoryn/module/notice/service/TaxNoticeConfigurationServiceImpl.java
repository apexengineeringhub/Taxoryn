package com.taxoryn.module.notice.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.exception.UnauthorizedException;
import com.taxoryn.core.security.SecurityUtils;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.notice.dto.TaxNoticeConfigDto;
import com.taxoryn.module.notice.dto.UpdateTaxNoticeConfigRequest;
import com.taxoryn.module.notice.entity.TaxNoticeConfigEntity;
import com.taxoryn.module.notice.enums.NoticePriority;
import com.taxoryn.module.notice.mapper.TaxNoticeConfigMapper;
import com.taxoryn.module.notice.repository.TaxNoticeConfigRepository;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.organization.entity.OrganizationType;
import com.taxoryn.module.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxNoticeConfigurationServiceImpl implements TaxNoticeConfigurationService {

    private final TaxNoticeConfigRepository configRepository;
    private final OrganizationRepository organizationRepository;
    private final TaxNoticeConfigMapper configMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public TaxNoticeConfigDto getEffectiveConfiguration() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required to resolve tax notice configuration");
        }
        return getEffectiveConfiguration(organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaxNoticeConfigDto getEffectiveConfiguration(UUID organizationId) {
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        OrganizationType orgType = org.getOrganizationType() != null ? org.getOrganizationType() : OrganizationType.UNKNOWN;

        // 1. Check for explicit persisted organization configuration
        Optional<TaxNoticeConfigEntity> customConfig = configRepository.findByOrganizationId(organizationId);
        if (customConfig.isPresent()) {
            return configMapper.toDto(customConfig.get(), orgType);
        }

        // 2. Fall back to persona default
        TaxNoticeConfigDto personaDefault = getPersonaDefaults(orgType);
        personaDefault.setOrganizationId(organizationId);
        personaDefault.setOrganizationType(orgType);
        personaDefault.setCustomized(false);
        return personaDefault;
    }

    @Override
    @Transactional
    public TaxNoticeConfigDto updateConfiguration(UpdateTaxNoticeConfigRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        if (organizationId == null) {
            throw new UnauthorizedException("Authenticated organization context is required to update tax notice configuration");
        }
        return updateConfiguration(organizationId, request);
    }

    @Override
    @Transactional
    public TaxNoticeConfigDto updateConfiguration(UUID organizationId, UpdateTaxNoticeConfigRequest request) {
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        OrganizationType orgType = org.getOrganizationType() != null ? org.getOrganizationType() : OrganizationType.UNKNOWN;

        TaxNoticeConfigEntity entity = configRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    TaxNoticeConfigDto defaults = getPersonaDefaults(orgType);
                    return TaxNoticeConfigEntity.builder()
                            .responseReviewRequired(defaults.isResponseReviewRequired())
                            .partnerApprovalRequired(defaults.isPartnerApprovalRequired())
                            .hearingTrackingEnabled(defaults.isHearingTrackingEnabled())
                            .responseSubmissionTrackingEnabled(defaults.isResponseSubmissionTrackingEnabled())
                            .defaultResponseDueDays(defaults.getDefaultResponseDueDays())
                            .reminderDaysBeforeDue(defaults.getReminderDaysBeforeDue())
                            .escalationDaysAfterDue(defaults.getEscalationDaysAfterDue())
                            .autoCreateResponseTask(defaults.isAutoCreateResponseTask())
                            .defaultPriority(defaults.getDefaultPriority() != null ? defaults.getDefaultPriority() : NoticePriority.MEDIUM)
                            .assignmentRequired(defaults.isAssignmentRequired())
                            .notifyOnAssignment(defaults.isNotifyOnAssignment())
                            .notifyOnDueSoon(defaults.isNotifyOnDueSoon())
                            .notifyOnOverdue(defaults.isNotifyOnOverdue())
                            .notifyOnSubmission(defaults.isNotifyOnSubmission())
                            .notifyOnHearing(defaults.isNotifyOnHearing())
                            .showDueSoon(defaults.isShowDueSoon())
                            .showOverdue(defaults.isShowOverdue())
                            .showAwaitingResponse(defaults.isShowAwaitingResponse())
                            .showAwaitingHearing(defaults.isShowAwaitingHearing())
                            .showAwaitingOrder(defaults.isShowAwaitingOrder())
                            .customized(true)
                            .build();
                });

        TaxNoticeConfigDto oldState = configMapper.toDto(entity, orgType);

        // Apply partial updates from request
        if (request.getResponseReviewRequired() != null) {
            entity.setResponseReviewRequired(request.getResponseReviewRequired());
        }
        if (request.getPartnerApprovalRequired() != null) {
            entity.setPartnerApprovalRequired(request.getPartnerApprovalRequired());
        }
        if (request.getHearingTrackingEnabled() != null) {
            entity.setHearingTrackingEnabled(request.getHearingTrackingEnabled());
        }
        if (request.getResponseSubmissionTrackingEnabled() != null) {
            entity.setResponseSubmissionTrackingEnabled(request.getResponseSubmissionTrackingEnabled());
        }
        if (request.getDefaultResponseDueDays() != null) {
            entity.setDefaultResponseDueDays(request.getDefaultResponseDueDays());
        }
        if (request.getReminderDaysBeforeDue() != null) {
            entity.setReminderDaysBeforeDue(request.getReminderDaysBeforeDue());
        }
        if (request.getEscalationDaysAfterDue() != null) {
            entity.setEscalationDaysAfterDue(request.getEscalationDaysAfterDue());
        }
        if (request.getAutoCreateResponseTask() != null) {
            entity.setAutoCreateResponseTask(request.getAutoCreateResponseTask());
        }
        if (request.getDefaultPriority() != null) {
            entity.setDefaultPriority(request.getDefaultPriority());
        }
        if (request.getAssignmentRequired() != null) {
            entity.setAssignmentRequired(request.getAssignmentRequired());
        }
        if (request.getNotifyOnAssignment() != null) {
            entity.setNotifyOnAssignment(request.getNotifyOnAssignment());
        }
        if (request.getNotifyOnDueSoon() != null) {
            entity.setNotifyOnDueSoon(request.getNotifyOnDueSoon());
        }
        if (request.getNotifyOnOverdue() != null) {
            entity.setNotifyOnOverdue(request.getNotifyOnOverdue());
        }
        if (request.getNotifyOnSubmission() != null) {
            entity.setNotifyOnSubmission(request.getNotifyOnSubmission());
        }
        if (request.getNotifyOnHearing() != null) {
            entity.setNotifyOnHearing(request.getNotifyOnHearing());
        }
        if (request.getShowDueSoon() != null) {
            entity.setShowDueSoon(request.getShowDueSoon());
        }
        if (request.getShowOverdue() != null) {
            entity.setShowOverdue(request.getShowOverdue());
        }
        if (request.getShowAwaitingResponse() != null) {
            entity.setShowAwaitingResponse(request.getShowAwaitingResponse());
        }
        if (request.getShowAwaitingHearing() != null) {
            entity.setShowAwaitingHearing(request.getShowAwaitingHearing());
        }
        if (request.getShowAwaitingOrder() != null) {
            entity.setShowAwaitingOrder(request.getShowAwaitingOrder());
        }

        entity.setCustomized(true);

        TaxNoticeConfigEntity saved = configRepository.save(entity);
        TaxNoticeConfigDto newState = configMapper.toDto(saved, orgType);

        auditService.logEvent("TAX_NOTICE_CONFIGURATION_UPDATED", "TAX_NOTICE_CONFIG",
                organizationId.toString(), oldState, newState);

        return newState;
    }

    @Override
    @Transactional
    public TaxNoticeConfigDto resetToPersonaDefaults(UUID organizationId) {
        OrganizationEntity org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        OrganizationType orgType = org.getOrganizationType() != null ? org.getOrganizationType() : OrganizationType.UNKNOWN;

        configRepository.findByOrganizationId(organizationId).ifPresent(entity -> {
            auditService.logEvent("TAX_NOTICE_CONFIGURATION_RESET", "TAX_NOTICE_CONFIG",
                    organizationId.toString(), entity, null);
            configRepository.delete(entity);
        });

        TaxNoticeConfigDto personaDefault = getPersonaDefaults(orgType);
        personaDefault.setOrganizationId(organizationId);
        personaDefault.setOrganizationType(orgType);
        personaDefault.setCustomized(false);
        return personaDefault;
    }

    @Override
    public TaxNoticeConfigDto getPersonaDefaults(OrganizationType organizationType) {
        OrganizationType type = organizationType != null ? organizationType : OrganizationType.UNKNOWN;

        return switch (type) {
            case SOLO_PRACTITIONER -> TaxNoticeConfigDto.builder()
                    .organizationType(OrganizationType.SOLO_PRACTITIONER)
                    .isCustomized(false)
                    .responseReviewRequired(false)
                    .partnerApprovalRequired(false)
                    .hearingTrackingEnabled(true)
                    .responseSubmissionTrackingEnabled(true)
                    .defaultResponseDueDays(30)
                    .reminderDaysBeforeDue(5)
                    .escalationDaysAfterDue(1)
                    .autoCreateResponseTask(false)
                    .defaultPriority(NoticePriority.MEDIUM)
                    .assignmentRequired(false)
                    .notifyOnAssignment(false)
                    .notifyOnDueSoon(true)
                    .notifyOnOverdue(true)
                    .notifyOnSubmission(false)
                    .notifyOnHearing(true)
                    .showDueSoon(true)
                    .showOverdue(true)
                    .showAwaitingResponse(true)
                    .showAwaitingHearing(true)
                    .showAwaitingOrder(false)
                    .build();

            case SMALL_TAX_FIRM -> TaxNoticeConfigDto.builder()
                    .organizationType(OrganizationType.SMALL_TAX_FIRM)
                    .isCustomized(false)
                    .responseReviewRequired(true)
                    .partnerApprovalRequired(false)
                    .hearingTrackingEnabled(true)
                    .responseSubmissionTrackingEnabled(true)
                    .defaultResponseDueDays(30)
                    .reminderDaysBeforeDue(7)
                    .escalationDaysAfterDue(2)
                    .autoCreateResponseTask(true)
                    .defaultPriority(NoticePriority.MEDIUM)
                    .assignmentRequired(true)
                    .notifyOnAssignment(true)
                    .notifyOnDueSoon(true)
                    .notifyOnOverdue(true)
                    .notifyOnSubmission(true)
                    .notifyOnHearing(true)
                    .showDueSoon(true)
                    .showOverdue(true)
                    .showAwaitingResponse(true)
                    .showAwaitingHearing(true)
                    .showAwaitingOrder(true)
                    .build();

            case GROWING_PRACTICE -> TaxNoticeConfigDto.builder()
                    .organizationType(OrganizationType.GROWING_PRACTICE)
                    .isCustomized(false)
                    .responseReviewRequired(true)
                    .partnerApprovalRequired(false)
                    .hearingTrackingEnabled(true)
                    .responseSubmissionTrackingEnabled(true)
                    .defaultResponseDueDays(30)
                    .reminderDaysBeforeDue(7)
                    .escalationDaysAfterDue(3)
                    .autoCreateResponseTask(true)
                    .defaultPriority(NoticePriority.MEDIUM)
                    .assignmentRequired(true)
                    .notifyOnAssignment(true)
                    .notifyOnDueSoon(true)
                    .notifyOnOverdue(true)
                    .notifyOnSubmission(true)
                    .notifyOnHearing(true)
                    .showDueSoon(true)
                    .showOverdue(true)
                    .showAwaitingResponse(true)
                    .showAwaitingHearing(true)
                    .showAwaitingOrder(true)
                    .build();

            case BUSINESS -> TaxNoticeConfigDto.builder()
                    .organizationType(OrganizationType.BUSINESS)
                    .isCustomized(false)
                    .responseReviewRequired(true)
                    .partnerApprovalRequired(false)
                    .hearingTrackingEnabled(true)
                    .responseSubmissionTrackingEnabled(true)
                    .defaultResponseDueDays(21)
                    .reminderDaysBeforeDue(10)
                    .escalationDaysAfterDue(2)
                    .autoCreateResponseTask(true)
                    .defaultPriority(NoticePriority.HIGH)
                    .assignmentRequired(true)
                    .notifyOnAssignment(true)
                    .notifyOnDueSoon(true)
                    .notifyOnOverdue(true)
                    .notifyOnSubmission(true)
                    .notifyOnHearing(true)
                    .showDueSoon(true)
                    .showOverdue(true)
                    .showAwaitingResponse(true)
                    .showAwaitingHearing(true)
                    .showAwaitingOrder(true)
                    .build();

            case UNKNOWN -> TaxNoticeConfigDto.builder()
                    .organizationType(OrganizationType.UNKNOWN)
                    .isCustomized(false)
                    .responseReviewRequired(true)
                    .partnerApprovalRequired(false)
                    .hearingTrackingEnabled(true)
                    .responseSubmissionTrackingEnabled(true)
                    .defaultResponseDueDays(30)
                    .reminderDaysBeforeDue(7)
                    .escalationDaysAfterDue(2)
                    .autoCreateResponseTask(true)
                    .defaultPriority(NoticePriority.MEDIUM)
                    .assignmentRequired(false)
                    .notifyOnAssignment(true)
                    .notifyOnDueSoon(true)
                    .notifyOnOverdue(true)
                    .notifyOnSubmission(true)
                    .notifyOnHearing(true)
                    .showDueSoon(true)
                    .showOverdue(true)
                    .showAwaitingResponse(true)
                    .showAwaitingHearing(true)
                    .showAwaitingOrder(true)
                    .build();
        };
    }
}
