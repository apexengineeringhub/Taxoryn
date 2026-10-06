package com.taxoryn.module.compliance.work.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.period.model.CompliancePeriod;
import com.taxoryn.module.compliance.period.service.CompliancePeriodService;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.repository.ComplianceRuleCatalogRepository;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationRequest;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationResultDto;
import com.taxoryn.module.compliance.work.dto.ComplianceWorkGenerationResultDto.GenerationStatus;
import com.taxoryn.module.compliance.work.port.ComplianceWorkGenerationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Compliance-domain orchestration service for Obligation → Work generation.
 *
 * <p>Responsibility chain:
 * <ol>
 *   <li>Validate the obligation exists and belongs to the tenant + client.</li>
 *   <li>Check obligation status eligibility.</li>
 *   <li>Resolve the rule and its {@code defaultWorkTemplateCode}.</li>
 *   <li>Resolve the compliance period's start/end dates.</li>
 *   <li>Call {@link ComplianceWorkGenerationPort} — the only way this service
 *       reaches the work module.</li>
 *   <li>On success, update {@code obligation.workInstanceId} for reverse traceability.</li>
 * </ol>
 *
 * <p>This service MUST NOT directly inject work-module repositories (WorkInstanceRepository,
 * TaskRepository). All work generation crosses the {@link ComplianceWorkGenerationPort} boundary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceWorkOrchestrationServiceImpl implements ComplianceWorkOrchestrationService {

    private final ComplianceObligationRepository obligationRepository;
    private final ComplianceRuleCatalogRepository ruleRepository;
    private final CompliancePeriodService periodService;
    private final ComplianceWorkGenerationPort workGenerationPort;
    private final AuditService auditService;

    /** Non-terminal, non-filed statuses eligible for work generation. */
    private static final java.util.Set<ComplianceObligationStatus> ELIGIBLE_STATUSES = java.util.Set.of(
            ComplianceObligationStatus.UPCOMING,
            ComplianceObligationStatus.READY,
            ComplianceObligationStatus.IN_PROGRESS,
            ComplianceObligationStatus.WAITING_FOR_CLIENT,
            ComplianceObligationStatus.READY_FOR_FILING,
            ComplianceObligationStatus.OVERDUE
    );

    @Override
    @Transactional
    public ComplianceWorkGenerationResultDto generateWorkForObligation(UUID clientId, UUID obligationId) {
        UUID orgId = TenantContext.getTenantId();

        // ---------------------------------------------------------------
        // 1. Load and validate obligation — tenant + client isolation
        // ---------------------------------------------------------------
        ComplianceObligationEntity obligation = obligationRepository
                .findByIdAndOrganizationId(obligationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceObligation", "id", obligationId));

        if (!clientId.equals(obligation.getClientId())) {
            throw new ResourceNotFoundException("ComplianceObligation", "id", obligationId);
        }

        // ---------------------------------------------------------------
        // 2. Status eligibility check
        // ---------------------------------------------------------------
        if (!ELIGIBLE_STATUSES.contains(obligation.getStatus())) {
            log.info("ComplianceWorkOrchestration: OBLIGATION_NOT_ELIGIBLE id={} status={}",
                    obligationId, obligation.getStatus());
            return ComplianceWorkGenerationResultDto.builder()
                    .status(GenerationStatus.OBLIGATION_NOT_ELIGIBLE)
                    .obligationId(obligationId)
                    .message("Obligation status '" + obligation.getStatus().getDisplayName()
                            + "' is not eligible for work generation.")
                    .build();
        }

        // ---------------------------------------------------------------
        // 3. Resolve rule and defaultWorkTemplateCode
        // ---------------------------------------------------------------
        String templateCode = resolveTemplateCode(obligation, orgId);

        // ---------------------------------------------------------------
        // 4. Resolve period dates from the obligation's periodType + periodKey
        // ---------------------------------------------------------------
        CompliancePeriod period = null;
        if (obligation.getPeriodType() != null && StringUtils.hasText(obligation.getPeriodKey())) {
            try {
                period = periodService.resolvePeriod(obligation.getPeriodType(), obligation.getPeriodKey());
            } catch (Exception ex) {

                log.warn("ComplianceWorkOrchestration: period resolution failed for obligationId={}: {}",
                        obligationId, ex.getMessage());
                // Non-fatal — periodStart/periodEnd will fallback inside the work service
            }
        }

        java.time.LocalDate periodStart = period != null ? period.getStartDate() : null;
        java.time.LocalDate periodEnd = period != null ? period.getEndDate() : null;
        String periodLabel = period != null ? period.getDisplayLabel() : obligation.getPeriodKey();

        // Due date: prefer internalTargetDate, fallback to statutoryDueDate
        java.time.LocalDate dueDate = obligation.getInternalTargetDate() != null
                ? obligation.getInternalTargetDate()
                : obligation.getStatutoryDueDate();

        // ---------------------------------------------------------------
        // 5. Build request and call port
        // ---------------------------------------------------------------
        ComplianceWorkGenerationRequest request = ComplianceWorkGenerationRequest.builder()
                .obligationId(obligationId)
                .clientId(clientId)
                .organizationId(orgId)
                .workTemplateCode(templateCode)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dueDate(dueDate)
                .clientServiceId(obligation.getClientServiceId())
                .periodLabel(periodLabel)
                .ruleCode(obligation.getRuleCode())
                .build();

        ComplianceWorkGenerationResultDto result = workGenerationPort.generateWork(request);

        // ---------------------------------------------------------------
        // 6. On success, update obligation's workInstanceId (reverse traceability)
        // ---------------------------------------------------------------
        if (result.getStatus().isSuccess() && result.getWorkInstanceId() != null) {
            if (obligation.getWorkInstanceId() == null) {
                obligation.setWorkInstanceId(result.getWorkInstanceId());
                obligationRepository.save(obligation);
                log.info("ComplianceWorkOrchestration: set workInstanceId={} on obligationId={}",
                        result.getWorkInstanceId(), obligationId);
            }

            auditService.logEvent("COMPLIANCE_WORK_GENERATION_SUCCESS",
                    "COMPLIANCE_OBLIGATION", obligationId.toString(),
                    null,
                    java.util.Map.of(
                            "workInstanceId", result.getWorkInstanceId().toString(),
                            "status", result.getStatus().name(),
                            "ruleCode", obligation.getRuleCode() != null ? obligation.getRuleCode() : ""
                    ));
        }

        return result;
    }

    // -----------------------------------------------------------------------
    // Resolve template code: tenant rule override wins over system rule
    // -----------------------------------------------------------------------
    private String resolveTemplateCode(ComplianceObligationEntity obligation, UUID orgId) {
        String ruleCode = obligation.getRuleCode();
        if (!StringUtils.hasText(ruleCode)) {
            return null;
        }

        // 1. Tenant-specific rule override
        Optional<ComplianceRuleEntity> tenantRule = ruleRepository
                .findByRuleCodeAndOrganizationId(ruleCode, orgId);
        if (tenantRule.isPresent()
                && StringUtils.hasText(tenantRule.get().getDefaultWorkTemplateCode())) {
            return tenantRule.get().getDefaultWorkTemplateCode();
        }

        // 2. System rule (organizationId IS NULL)
        Optional<ComplianceRuleEntity> systemRule = ruleRepository
                .findByRuleCodeAndOrganizationIdIsNull(ruleCode);
        if (systemRule.isPresent()
                && StringUtils.hasText(systemRule.get().getDefaultWorkTemplateCode())) {
            return systemRule.get().getDefaultWorkTemplateCode();
        }

        // 3. Not configured
        return null;
    }
}
