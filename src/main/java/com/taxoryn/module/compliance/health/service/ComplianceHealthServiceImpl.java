package com.taxoryn.module.compliance.health.service;

import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.applicability.dto.ComplianceApplicabilitySummaryDto;
import com.taxoryn.module.compliance.applicability.service.ComplianceApplicabilityService;
import com.taxoryn.module.compliance.calendar.service.ComplianceDeadlineClassifier;
import com.taxoryn.module.compliance.calendar.service.ComplianceDeadlineClassifier.ClassificationResult;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.health.dto.ClientComplianceHealthDto;
import com.taxoryn.module.compliance.health.dto.ComplianceRecommendationDto;
import com.taxoryn.module.compliance.health.dto.ComplianceRiskSignalDto;
import com.taxoryn.module.compliance.health.dto.PracticeComplianceHealthSummaryDto;
import com.taxoryn.module.compliance.health.model.ComplianceActionCode;
import com.taxoryn.module.compliance.health.model.ComplianceHealthStatus;
import com.taxoryn.module.compliance.health.model.ComplianceRiskSeverity;
import com.taxoryn.module.compliance.health.model.ComplianceRiskSignalCode;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.profile.dto.ComplianceProfileDto;
import com.taxoryn.module.compliance.profile.service.ComplianceProfileService;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.repository.ComplianceRuleCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic, explainable, read-only implementation of Compliance Health & Intelligence.
 * Pure query layer over existing authoritative compliance models with zero write side-effects.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplianceHealthServiceImpl implements ComplianceHealthService {

    private final ClientRepository clientRepository;
    private final ComplianceObligationRepository obligationRepository;
    private final ComplianceRuleCatalogRepository ruleRepository;
    private final ComplianceProfileService profileService;
    private final com.taxoryn.module.compliance.profile.repository.ComplianceProfileRepository complianceProfileRepository;
    private final ComplianceApplicabilityService applicabilityService;
    private final ComplianceDeadlineClassifier deadlineClassifier;

    @Override
    public ClientComplianceHealthDto getClientComplianceHealth(UUID clientId, LocalDate referenceDate) {
        UUID orgId = TenantContext.getTenantId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: '" + clientId + "'"));

        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Instant now = Instant.now();

        // 1. Load active & tracked obligations
        List<ComplianceObligationEntity> obligations = obligationRepository.findAllByOrganizationIdAndClientId(orgId, clientId);

        // 2. Check if compliance profile entity actually exists in DB
        boolean hasProfileInDb = complianceProfileRepository.existsByOrganizationIdAndClientId(orgId, clientId);
        ComplianceProfileDto profile = null;
        if (hasProfileInDb) {
            try {
                profile = profileService.getComplianceProfile(clientId);
            } catch (Exception ex) {
                log.debug("Compliance profile lookup failed for client {}: {}", clientId, ex.getMessage());
            }
        }

        // 3. Load applicability summary if available
        ComplianceApplicabilitySummaryDto applicabilitySummary = null;
        try {
            applicabilitySummary = applicabilityService.getApplicabilitySummary(clientId, refDate);
        } catch (Exception ex) {
            log.debug("Applicability summary lookup failed for client {}: {}", clientId, ex.getMessage());
        }

        // 4. Evaluate risk signals and counts
        List<ComplianceRiskSignalDto> signals = new ArrayList<>();
        List<String> factors = new ArrayList<>();

        long overdueCount = 0;
        long dueTodayCount = 0;
        long dueWithin3DaysCount = 0;
        long workNotGeneratedCount = 0;
        long missingDueDateCount = 0;
        long openObligations = 0;

        // Map rules by code for fast template lookup
        Map<String, ComplianceRuleEntity> rulesByCode = new HashMap<>();

        for (ComplianceObligationEntity obl : obligations) {
            // Exclude terminal / completed / cancelled obligations from active risk
            if (obl.getStatus() == ComplianceObligationStatus.COMPLETED ||
                obl.getStatus() == ComplianceObligationStatus.CANCELLED) {
                continue;
            }

            openObligations++;

            LocalDate dueDate = obl.getStatutoryDueDate() != null ? obl.getStatutoryDueDate() : obl.getDueDate();
            ClassificationResult classification = deadlineClassifier.classify(obl, refDate);

            // A. Overdue Obligation
            if (classification.status() == com.taxoryn.module.compliance.calendar.model.DeadlineStatus.OVERDUE) {
                overdueCount++;
                long daysOverdue = classification.daysOverdue();
                ComplianceRiskSeverity severity = daysOverdue > 7 ? ComplianceRiskSeverity.CRITICAL : ComplianceRiskSeverity.HIGH;
                int priority = daysOverdue > 7 ? 100 : 85;

                signals.add(ComplianceRiskSignalDto.builder()
                        .signalCode(ComplianceRiskSignalCode.OVERDUE_OBLIGATION)
                        .severity(severity)
                        .priority(priority)
                        .title("Overdue statutory obligation: " + (obl.getRuleNameSnapshot() != null ? obl.getRuleNameSnapshot() : obl.getRuleCode()))
                        .explanation("Obligation for period " + obl.getPeriodKey() + " was due on " + dueDate + " and is " + daysOverdue + " days overdue.")
                        .clientId(clientId)
                        .obligationId(obl.getId())
                        .ruleCode(obl.getRuleCode())
                        .domain(obl.getDomain())
                        .periodKey(obl.getPeriodKey())
                        .dueDate(dueDate)
                        .workInstanceId(obl.getWorkInstanceId())
                        .actionCode(obl.getWorkInstanceId() == null ? ComplianceActionCode.GENERATE_WORK : ComplianceActionCode.REVIEW_OVERDUE_OBLIGATION)
                        .recommendedAction(obl.getWorkInstanceId() == null ? "Generate work immediately for overdue filing" : "Review and complete overdue work execution")
                        .build());
            }
            // B. Due Today
            else if (classification.status() == com.taxoryn.module.compliance.calendar.model.DeadlineStatus.DUE_TODAY) {
                dueTodayCount++;
                signals.add(ComplianceRiskSignalDto.builder()
                        .signalCode(ComplianceRiskSignalCode.DUE_TODAY)
                        .severity(ComplianceRiskSeverity.HIGH)
                        .priority(80)
                        .title("Statutory deadline is TODAY: " + (obl.getRuleNameSnapshot() != null ? obl.getRuleNameSnapshot() : obl.getRuleCode()))
                        .explanation("Statutory obligation for period " + obl.getPeriodKey() + " is due today (" + dueDate + ").")
                        .clientId(clientId)
                        .obligationId(obl.getId())
                        .ruleCode(obl.getRuleCode())
                        .domain(obl.getDomain())
                        .periodKey(obl.getPeriodKey())
                        .dueDate(dueDate)
                        .workInstanceId(obl.getWorkInstanceId())
                        .actionCode(obl.getWorkInstanceId() == null ? ComplianceActionCode.GENERATE_WORK : ComplianceActionCode.START_WORK)
                        .recommendedAction(obl.getWorkInstanceId() == null ? "Generate work for today's deadline" : "Complete statutory filing today")
                        .build());
            }
            // C. Due Soon (within 3 days)
            else if (classification.status() == com.taxoryn.module.compliance.calendar.model.DeadlineStatus.DUE_TOMORROW ||
                     classification.status() == com.taxoryn.module.compliance.calendar.model.DeadlineStatus.DUE_WITHIN_3_DAYS) {
                dueWithin3DaysCount++;
                long remaining = classification.daysRemaining();
                signals.add(ComplianceRiskSignalDto.builder()
                        .signalCode(ComplianceRiskSignalCode.DUE_SOON)
                        .severity(obl.getWorkInstanceId() == null ? ComplianceRiskSeverity.HIGH : ComplianceRiskSeverity.MEDIUM)
                        .priority(70)
                        .title("Approaching deadline (" + remaining + " days left): " + (obl.getRuleNameSnapshot() != null ? obl.getRuleNameSnapshot() : obl.getRuleCode()))
                        .explanation("Statutory obligation is due on " + dueDate + " in " + remaining + " day(s).")
                        .clientId(clientId)
                        .obligationId(obl.getId())
                        .ruleCode(obl.getRuleCode())
                        .domain(obl.getDomain())
                        .periodKey(obl.getPeriodKey())
                        .dueDate(dueDate)
                        .workInstanceId(obl.getWorkInstanceId())
                        .actionCode(obl.getWorkInstanceId() == null ? ComplianceActionCode.GENERATE_WORK : ComplianceActionCode.START_WORK)
                        .recommendedAction(obl.getWorkInstanceId() == null ? "Generate work package ahead of deadline" : "Proceed with preparation and filing")
                        .build());
            }

            // D. Work Not Generated
            if (obl.getWorkInstanceId() == null) {
                workNotGeneratedCount++;
                ComplianceRiskSeverity severity = (dueDate != null && (dueDate.isBefore(refDate) || !dueDate.isAfter(refDate.plusDays(3))))
                        ? ComplianceRiskSeverity.HIGH
                        : ComplianceRiskSeverity.MEDIUM;
                int priority = (dueDate != null && dueDate.isBefore(refDate)) ? 75 : 50;

                // Check if template code is configured on rule
                String ruleCode = obl.getRuleCode();
                if (ruleCode != null && !rulesByCode.containsKey(ruleCode)) {
                    Optional<ComplianceRuleEntity> ruleOpt = ruleRepository.findByRuleCodeAndOrganizationId(ruleCode, orgId)
                            .or(() -> ruleRepository.findByRuleCodeAndOrganizationIdIsNull(ruleCode));
                    ruleOpt.ifPresent(r -> rulesByCode.put(ruleCode, r));
                }
                ComplianceRuleEntity ruleEntity = rulesByCode.get(ruleCode);
                boolean hasTemplate = ruleEntity != null && StringUtils.hasText(ruleEntity.getDefaultWorkTemplateCode());

                if (!hasTemplate) {
                    signals.add(ComplianceRiskSignalDto.builder()
                            .signalCode(ComplianceRiskSignalCode.NO_WORK_TEMPLATE)
                            .severity(ComplianceRiskSeverity.MEDIUM)
                            .priority(45)
                            .title("No work template configured for rule: " + ruleCode)
                            .explanation("Rule '" + ruleCode + "' has no work template assigned; work cannot be automatically generated.")
                            .clientId(clientId)
                            .obligationId(obl.getId())
                            .ruleCode(ruleCode)
                            .domain(obl.getDomain())
                            .periodKey(obl.getPeriodKey())
                            .dueDate(dueDate)
                            .actionCode(ComplianceActionCode.CONFIGURE_WORK_TEMPLATE)
                            .recommendedAction("Configure default work template for rule " + ruleCode)
                            .build());
                } else {
                    signals.add(ComplianceRiskSignalDto.builder()
                            .signalCode(ComplianceRiskSignalCode.WORK_NOT_GENERATED)
                            .severity(severity)
                            .priority(priority)
                            .title("Work package not generated: " + (obl.getRuleNameSnapshot() != null ? obl.getRuleNameSnapshot() : obl.getRuleCode()))
                            .explanation("Obligation is active for period " + obl.getPeriodKey() + " but no corresponding work instance has been generated.")
                            .clientId(clientId)
                            .obligationId(obl.getId())
                            .ruleCode(ruleCode)
                            .domain(obl.getDomain())
                            .periodKey(obl.getPeriodKey())
                            .dueDate(dueDate)
                            .actionCode(ComplianceActionCode.GENERATE_WORK)
                            .recommendedAction("Generate practice work instance for " + (obl.getRuleNameSnapshot() != null ? obl.getRuleNameSnapshot() : obl.getRuleCode()))
                            .build());
                }
            }

            // E. Missing Due Date
            if (dueDate == null) {
                missingDueDateCount++;
                signals.add(ComplianceRiskSignalDto.builder()
                        .signalCode(ComplianceRiskSignalCode.MISSING_DUE_DATE)
                        .severity(ComplianceRiskSeverity.MEDIUM)
                        .priority(40)
                        .title("Missing statutory due date: " + (obl.getRuleNameSnapshot() != null ? obl.getRuleNameSnapshot() : obl.getRuleCode()))
                        .explanation("Statutory due date is not calculated or configured for period " + obl.getPeriodKey() + ".")
                        .clientId(clientId)
                        .obligationId(obl.getId())
                        .ruleCode(obl.getRuleCode())
                        .domain(obl.getDomain())
                        .periodKey(obl.getPeriodKey())
                        .actionCode(ComplianceActionCode.CALCULATE_DUE_DATE)
                        .recommendedAction("Recalculate or configure statutory due date")
                        .build());
            }
        }

        // F. Missing Compliance Profile
        if (profile == null || (profile.getGstConfig() == null && profile.getTdsConfig() == null && profile.getItrConfig() == null)) {
            signals.add(ComplianceRiskSignalDto.builder()
                    .signalCode(ComplianceRiskSignalCode.MISSING_COMPLIANCE_PROFILE)
                    .severity(ComplianceRiskSeverity.MEDIUM)
                    .priority(35)
                    .title("Incomplete compliance profile")
                    .explanation("Client has no active compliance profile configured; automatic applicability evaluation is limited.")
                    .clientId(clientId)
                    .actionCode(ComplianceActionCode.UPDATE_COMPLIANCE_PROFILE)
                    .recommendedAction("Complete client statutory compliance profile facts")
                    .build());
        }

        // G. Insufficient Applicability Data
        long insufficientDataCount = 0;
        if (applicabilitySummary != null && applicabilitySummary.getInsufficientDataCount() > 0) {
            insufficientDataCount = applicabilitySummary.getInsufficientDataCount();
            signals.add(ComplianceRiskSignalDto.builder()
                    .signalCode(ComplianceRiskSignalCode.INSUFFICIENT_APPLICABILITY_DATA)
                    .severity(ComplianceRiskSeverity.LOW)
                    .priority(30)
                    .title(insufficientDataCount + " rule(s) require additional client information")
                    .explanation("Applicability evaluator needs more tax/profile facts to determine whether " + insufficientDataCount + " rule(s) apply.")
                    .clientId(clientId)
                    .actionCode(ComplianceActionCode.RESOLVE_APPLICABILITY_DATA)
                    .recommendedAction("Provide required tax facts to finalize compliance rules applicability")
                    .build());
        }

        // 5. Calculate Deterministic Health Score (0–100)
        // If there are zero obligations and no profile -> NO_DATA
        if (obligations.isEmpty() && profile == null) {
            return ClientComplianceHealthDto.builder()
                    .clientId(clientId)
                    .clientDisplayName(client.getDisplayName())
                    .healthScore(100)
                    .healthStatus(ComplianceHealthStatus.NO_DATA)
                    .contributingFactors(List.of("No compliance obligations or profile facts found for this client"))
                    .totalObligations(0)
                    .openObligations(0)
                    .overdueCount(0)
                    .dueTodayCount(0)
                    .dueWithin3DaysCount(0)
                    .workNotGeneratedCount(0)
                    .missingDueDateCount(0)
                    .insufficientDataCount(0)
                    .criticalRiskCount(0)
                    .highRiskCount(0)
                    .risks(Collections.emptyList())
                    .recommendations(Collections.emptyList())
                    .calculatedAt(now)
                    .referenceDate(refDate)
                    .build();
        }

        int score = 100;
        if (overdueCount > 0) {
            score -= Math.min(50, overdueCount * 25);
            factors.add(overdueCount + " overdue statutory obligation(s)");
        }
        if (dueTodayCount > 0) {
            score -= Math.min(15, dueTodayCount * 10);
            factors.add(dueTodayCount + " obligation(s) due today");
        }
        if (dueWithin3DaysCount > 0) {
            score -= Math.min(10, dueWithin3DaysCount * 5);
            factors.add(dueWithin3DaysCount + " obligation(s) due within 3 days");
        }
        if (workNotGeneratedCount > 0) {
            score -= Math.min(20, workNotGeneratedCount * 10);
            factors.add(workNotGeneratedCount + " obligation(s) without generated work packages");
        }
        if (missingDueDateCount > 0) {
            score -= Math.min(10, missingDueDateCount * 5);
            factors.add(missingDueDateCount + " obligation(s) missing statutory due dates");
        }
        if (hasProfileInDb && insufficientDataCount > 0) {
            score -= Math.min(10, insufficientDataCount * 2);
            factors.add(insufficientDataCount + " rule(s) with insufficient applicability data");
        }

        score = Math.max(0, Math.min(100, score));

        // Status classification:
        // 90-100: EXCELLENT
        // 75-89: GOOD
        // 50-74: AT_RISK
        // 0-49: CRITICAL
        ComplianceHealthStatus status;
        if (score >= 90) {
            status = ComplianceHealthStatus.EXCELLENT;
        } else if (score >= 75) {
            status = ComplianceHealthStatus.GOOD;
        } else if (score >= 50) {
            status = ComplianceHealthStatus.AT_RISK;
        } else {
            status = ComplianceHealthStatus.CRITICAL;
        }

        if (factors.isEmpty()) {
            factors.add("All statutory obligations and compliance facts are up to date and healthy");
        }

        // Sort signals deterministically by priority DESC, then title ASC
        signals.sort(Comparator.comparingInt(ComplianceRiskSignalDto::getPriority).reversed()
                .thenComparing(ComplianceRiskSignalDto::getTitle));

        // 6. Generate Deduplicated Top Recommendations (Max 5)
        List<ComplianceRecommendationDto> recommendations = new ArrayList<>();
        Set<String> seenActionKeys = new HashSet<>();

        for (ComplianceRiskSignalDto sig : signals) {
            if (recommendations.size() >= 5) {
                break;
            }
            if (sig.getActionCode() == null) {
                continue;
            }

            // Deduplication key per action and target entity
            String dedupeKey = sig.getActionCode().name() + "_" + (sig.getObligationId() != null ? sig.getObligationId() : sig.getClientId());
            if (seenActionKeys.add(dedupeKey)) {
                recommendations.add(ComplianceRecommendationDto.builder()
                        .actionCode(sig.getActionCode())
                        .title(sig.getRecommendedAction())
                        .explanation(sig.getExplanation())
                        .severity(sig.getSeverity())
                        .priority(sig.getPriority())
                        .clientId(sig.getClientId())
                        .obligationId(sig.getObligationId())
                        .ruleCode(sig.getRuleCode())
                        .domain(sig.getDomain())
                        .targetType(sig.getObligationId() != null ? "COMPLIANCE_OBLIGATION" : "CLIENT")
                        .targetId(sig.getObligationId() != null ? sig.getObligationId() : sig.getClientId())
                        .build());
            }
        }

        long criticalRiskCount = signals.stream().filter(s -> s.getSeverity() == ComplianceRiskSeverity.CRITICAL).count();
        long highRiskCount = signals.stream().filter(s -> s.getSeverity() == ComplianceRiskSeverity.HIGH).count();

        return ClientComplianceHealthDto.builder()
                .clientId(clientId)
                .clientDisplayName(client.getDisplayName())
                .healthScore(score)
                .healthStatus(status)
                .contributingFactors(factors)
                .totalObligations(obligations.size())
                .openObligations(openObligations)
                .overdueCount(overdueCount)
                .dueTodayCount(dueTodayCount)
                .dueWithin3DaysCount(dueWithin3DaysCount)
                .workNotGeneratedCount(workNotGeneratedCount)
                .missingDueDateCount(missingDueDateCount)
                .insufficientDataCount(insufficientDataCount)
                .criticalRiskCount(criticalRiskCount)
                .highRiskCount(highRiskCount)
                .risks(signals)
                .recommendations(recommendations)
                .calculatedAt(now)
                .referenceDate(refDate)
                .build();
    }

    @Override
    public PracticeComplianceHealthSummaryDto getPracticeComplianceHealth(LocalDate referenceDate) {
        UUID orgId = TenantContext.getTenantId();
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Instant now = Instant.now();

        // Load all clients of this tenant
        List<ClientEntity> clients = clientRepository.findAllByOrganizationId(orgId);

        long totalActiveObligations = 0;
        long totalOverdueObligations = 0;
        long totalDueTodayObligations = 0;
        long totalDueWithin3DaysObligations = 0;
        long totalWorkNotGeneratedObligations = 0;

        long criticalClients = 0;
        long atRiskClients = 0;
        long goodClients = 0;
        long excellentClients = 0;

        List<ClientComplianceHealthDto> atRiskList = new ArrayList<>();

        for (ClientEntity client : clients) {
            ClientComplianceHealthDto health = getClientComplianceHealth(client.getId(), refDate);
            totalActiveObligations += health.getOpenObligations();
            totalOverdueObligations += health.getOverdueCount();
            totalDueTodayObligations += health.getDueTodayCount();
            totalDueWithin3DaysObligations += health.getDueWithin3DaysCount();
            totalWorkNotGeneratedObligations += health.getWorkNotGeneratedCount();

            switch (health.getHealthStatus()) {
                case CRITICAL -> {
                    criticalClients++;
                    atRiskList.add(health);
                }
                case AT_RISK -> {
                    atRiskClients++;
                    atRiskList.add(health);
                }
                case GOOD -> goodClients++;
                case EXCELLENT -> excellentClients++;
                case NO_DATA -> {}
            }
        }

        // Sort top at-risk clients by score ASC (lowest first), then overdue DESC
        atRiskList.sort(Comparator.comparingInt(ClientComplianceHealthDto::getHealthScore)
                .thenComparing(Comparator.comparingLong(ClientComplianceHealthDto::getOverdueCount).reversed()));

        List<ClientComplianceHealthDto> topAtRisk = atRiskList.stream().limit(5).toList();

        return PracticeComplianceHealthSummaryDto.builder()
                .organizationId(orgId)
                .totalClients(clients.size())
                .totalActiveObligations(totalActiveObligations)
                .totalOverdueObligations(totalOverdueObligations)
                .totalDueTodayObligations(totalDueTodayObligations)
                .totalDueWithin3DaysObligations(totalDueWithin3DaysObligations)
                .totalWorkNotGeneratedObligations(totalWorkNotGeneratedObligations)
                .criticalClientsCount(criticalClients)
                .atRiskClientsCount(atRiskClients)
                .goodClientsCount(goodClients)
                .excellentClientsCount(excellentClients)
                .topAtRiskClients(topAtRisk)
                .calculatedAt(now)
                .referenceDate(refDate)
                .build();
    }
}
