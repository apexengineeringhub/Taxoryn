package com.taxoryn.module.compliance.duedate.service;

import com.taxoryn.core.exception.BadRequestException;
import com.taxoryn.core.exception.ResourceNotFoundException;
import com.taxoryn.core.security.TenantContext;
import com.taxoryn.module.audit.service.AuditService;
import com.taxoryn.module.client.entity.ClientEntity;
import com.taxoryn.module.client.repository.ClientRepository;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.duedate.dto.DueDateCalculationResult;
import com.taxoryn.module.compliance.duedate.dto.ObligationDueDateDto;
import com.taxoryn.module.compliance.duedate.model.DueDateCalculationStatus;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.period.model.CompliancePeriod;
import com.taxoryn.module.compliance.period.service.CompliancePeriodService;
import com.taxoryn.module.compliance.repository.ComplianceObligationRepository;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleFrequency;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import com.taxoryn.module.compliance.rule.repository.ComplianceRuleCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceDueDateServiceImpl implements ComplianceDueDateService {

    private final ComplianceRuleCatalogRepository ruleRepository;
    private final ComplianceObligationRepository obligationRepository;
    private final ClientRepository clientRepository;
    private final CompliancePeriodService periodService;
    private final AuditService auditService;

    @Override
    public DueDateCalculationResult calculateDueDate(ComplianceRuleEntity rule, CompliancePeriod period, LocalDate asOfDate) {
        if (rule == null) {
            return DueDateCalculationResult.notConfigured(null, 1, "Rule configuration is null.");
        }
        if (period == null) {
            return DueDateCalculationResult.invalidConfig(rule.getRuleCode(), 1, "Compliance period is null.");
        }

        // 1. Statutory Validity Window Check
        if (rule.getEffectiveFrom() != null && period.getEndDate().isBefore(rule.getEffectiveFrom())) {
            return DueDateCalculationResult.outOfEffectiveRange(
                    rule.getRuleCode(), 1,
                    "Period end date " + period.getEndDate() + " is before rule effective date " + rule.getEffectiveFrom()
            );
        }
        if (rule.getEffectiveTo() != null && period.getStartDate().isAfter(rule.getEffectiveTo())) {
            return DueDateCalculationResult.outOfEffectiveRange(
                    rule.getRuleCode(), 1,
                    "Period start date " + period.getStartDate() + " is after rule expiration date " + rule.getEffectiveTo()
            );
        }

        // 2. Due Date Strategy Check
        DueDateRuleType strategy = rule.getDueDateRuleType();
        if (strategy == null) {
            return DueDateCalculationResult.notConfigured(rule.getRuleCode(), 1, "No due date strategy configured on rule.");
        }

        int graceDays = rule.getStatutoryGraceDays();

        // 3. Generic Strategy Interpretation
        switch (strategy) {
            case DAY_OF_FOLLOWING_MONTH -> {
                int dueDay = rule.getDueDayOffset() != null ? rule.getDueDayOffset() : (rule.getDueDay() != null ? rule.getDueDay() : 20);
                int monthOffset = rule.getDueMonthOffset() != null ? rule.getDueMonthOffset() : 1;

                YearMonth targetYm = YearMonth.from(period.getEndDate()).plusMonths(monthOffset);
                int day = Math.min(dueDay, targetYm.lengthOfMonth());
                LocalDate calculatedDate = targetYm.atDay(day);

                if (graceDays > 0) {
                    calculatedDate = calculatedDate.plusDays(graceDays);
                }

                String trace = "DAY_OF_FOLLOWING_MONTH [dueDay=" + dueDay + ", monthOffset=" + monthOffset + ", periodEnd=" + period.getEndDate() + " -> " + calculatedDate + "]";
                String explanation = (rule.getDueDateDescription() != null && !rule.getDueDateDescription().isEmpty())
                        ? rule.getDueDateDescription()
                        : ("Day " + dueDay + " of month following period end (" + calculatedDate + ")");

                return DueDateCalculationResult.calculated(
                        calculatedDate, strategy, rule.getRuleCode(), 1, explanation, trace, graceDays
                );
            }

            case DAY_OF_FOLLOWING_QUARTER_END_MONTH -> {
                int dueDay = rule.getDueDayOffset() != null ? rule.getDueDayOffset() : (rule.getDueDay() != null ? rule.getDueDay() : 31);
                int monthOffset = rule.getDueMonthOffset() != null ? rule.getDueMonthOffset() : 1;

                YearMonth targetYm = YearMonth.from(period.getEndDate()).plusMonths(monthOffset);
                int day = Math.min(dueDay, targetYm.lengthOfMonth());
                LocalDate calculatedDate = targetYm.atDay(day);

                if (graceDays > 0) {
                    calculatedDate = calculatedDate.plusDays(graceDays);
                }

                String trace = "DAY_OF_FOLLOWING_QUARTER_END_MONTH [dueDay=" + dueDay + ", monthOffset=" + monthOffset + ", periodEnd=" + period.getEndDate() + " -> " + calculatedDate + "]";
                String explanation = (rule.getDueDateDescription() != null && !rule.getDueDateDescription().isEmpty())
                        ? rule.getDueDateDescription()
                        : ("Day " + dueDay + " of month following quarter end (" + calculatedDate + ")");

                return DueDateCalculationResult.calculated(
                        calculatedDate, strategy, rule.getRuleCode(), 1, explanation, trace, graceDays
                );
            }

            case FIXED_DATE_IN_YEAR -> {
                Integer fixedMonth = rule.getFixedMonth();
                Integer fixedDay = rule.getFixedDay() != null ? rule.getFixedDay() : rule.getDueDay();

                if (fixedMonth == null || fixedDay == null) {
                    return DueDateCalculationResult.notConfigured(
                            rule.getRuleCode(), 1,
                            "Rule specifies FIXED_DATE_IN_YEAR but fixedMonth or fixedDay is not configured."
                    );
                }

                if (fixedMonth < 1 || fixedMonth > 12) {
                    return DueDateCalculationResult.invalidConfig(
                            rule.getRuleCode(), 1,
                            "Invalid fixed month: " + fixedMonth + " (must be between 1 and 12)."
                    );
                }

                int targetYear;
                if (period.getPeriodType() == CompliancePeriodType.ASSESSMENT_YEAR) {
                    int ayStartYear = Integer.parseInt(period.getPeriodKey().substring(0, 4));
                    targetYear = ayStartYear;
                } else if (period.getPeriodType() == CompliancePeriodType.FINANCIAL_YEAR) {
                    int fyStartYear = Integer.parseInt(period.getPeriodKey().substring(0, 4));
                    if (fixedMonth >= 4) {
                        // Annual filings after FY completion (e.g. GSTR-9, AOC-4, MGT-7, DIR-3 KYC) are due in fyStartYear + 1
                        // Periodic/interim filings during FY (e.g. Advance Tax Q1-Q3) are due in fyStartYear
                        if (rule.getFrequency() == ComplianceRuleFrequency.ANNUAL) {
                            targetYear = fyStartYear + 1;
                        } else {
                            targetYear = fyStartYear;
                        }
                    } else {
                        // Jan-Mar filings (e.g. Advance tax Q4) fall in calendar year fyStartYear + 1
                        targetYear = fyStartYear + 1;
                    }
                } else {
                    targetYear = period.getEndDate().getYear();
                }

                YearMonth targetYm = YearMonth.of(targetYear, fixedMonth);
                int day = Math.min(fixedDay, targetYm.lengthOfMonth());
                LocalDate calculatedDate = targetYm.atDay(day);

                if (graceDays > 0) {
                    calculatedDate = calculatedDate.plusDays(graceDays);
                }

                String trace = "FIXED_DATE_IN_YEAR [fixedMonth=" + fixedMonth + ", fixedDay=" + fixedDay + ", targetYear=" + targetYear + " -> " + calculatedDate + "]";
                String explanation = (rule.getDueDateDescription() != null && !rule.getDueDateDescription().isEmpty())
                        ? rule.getDueDateDescription()
                        : ("Fixed statutory date: " + calculatedDate + " for " + period.getDisplayLabel());

                return DueDateCalculationResult.calculated(
                        calculatedDate, strategy, rule.getRuleCode(), 1, explanation, trace, graceDays
                );
            }

            case CUSTOM_OFFSET_DAYS -> {
                int offsetDays = rule.getDueDayOffset() != null ? rule.getDueDayOffset() : 0;
                LocalDate calculatedDate = period.getEndDate().plusDays(offsetDays);

                if (graceDays > 0) {
                    calculatedDate = calculatedDate.plusDays(graceDays);
                }

                String trace = "CUSTOM_OFFSET_DAYS [offset=" + offsetDays + ", periodEnd=" + period.getEndDate() + " -> " + calculatedDate + "]";
                String explanation = offsetDays + " days following period end (" + calculatedDate + ")";

                return DueDateCalculationResult.calculated(
                        calculatedDate, strategy, rule.getRuleCode(), 1, explanation, trace, graceDays
                );
            }

            case SPECIFIC_DATE -> {
                if (rule.getEffectiveTo() != null) {
                    return DueDateCalculationResult.calculated(
                            rule.getEffectiveTo(), strategy, rule.getRuleCode(), 1,
                            "Specific statutory deadline: " + rule.getEffectiveTo(),
                            "SPECIFIC_DATE -> " + rule.getEffectiveTo(), graceDays
                    );
                }
                return DueDateCalculationResult.notConfigured(rule.getRuleCode(), 1, "Specific statutory date is not specified in rule.");
            }
        }

        return DueDateCalculationResult.notConfigured(rule.getRuleCode(), 1, "Unsupported due date strategy: " + strategy);
    }

    @Override
    public DueDateCalculationResult calculateDueDateForObligation(ComplianceObligationEntity obligation) {
        if (obligation == null) {
            return DueDateCalculationResult.notConfigured(null, 1, "Obligation is null.");
        }

        UUID orgId = obligation.getOrganizationId();
        String ruleCode = obligation.getRuleCode();
        if (ruleCode == null || ruleCode.trim().isEmpty()) {
            return DueDateCalculationResult.notConfigured(null, 1, "Obligation has no rule code.");
        }

        // Resolve rule (tenant custom or system standard)
        Optional<ComplianceRuleEntity> ruleOpt = ruleRepository.findByRuleCodeAndOrganizationIdIsNull(ruleCode);
        if (orgId != null) {
            Optional<ComplianceRuleEntity> tenantRuleOpt = ruleRepository.findByRuleCodeAndOrganizationId(ruleCode, orgId);
            if (tenantRuleOpt.isPresent()) {
                ruleOpt = tenantRuleOpt;
            }
        }

        if (ruleOpt.isEmpty()) {
            return DueDateCalculationResult.notConfigured(ruleCode, 1, "Rule '" + ruleCode + "' not found in catalog.");
        }

        CompliancePeriod period = periodService.resolvePeriod(obligation.getPeriodType(), obligation.getPeriodKey());
        return calculateDueDate(ruleOpt.get(), period, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public ObligationDueDateDto getObligationDueDate(UUID clientId, UUID obligationId) {
        UUID orgId = TenantContext.getTenantId();
        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(obligationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found with id: '" + obligationId + "'"));

        if (!obligation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("Compliance obligation not found for client id: '" + clientId + "'");
        }

        DueDateCalculationResult calcResult = calculateDueDateForObligation(obligation);

        return ObligationDueDateDto.builder()
                .obligationId(obligation.getId())
                .clientId(obligation.getClientId())
                .ruleCode(obligation.getRuleCode())
                .ruleVersion(obligation.getRuleVersion())
                .ruleName(obligation.getRuleNameSnapshot())
                .periodType(obligation.getPeriodType())
                .periodKey(obligation.getPeriodKey())
                .periodLabel(obligation.getPeriodLabel())
                .statutoryDueDate(obligation.getStatutoryDueDate() != null ? obligation.getStatutoryDueDate() : calcResult.getStatutoryDueDate())
                .dueDate(obligation.getDueDate() != null ? obligation.getDueDate() : calcResult.getDueDate())
                .calculationStatus(calcResult.getStatus())
                .strategy(calcResult.getStrategy())
                .explanation(calcResult.getExplanation())
                .calculatedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional
    public ComplianceObligationDto recalculateObligationDueDate(UUID clientId, UUID obligationId) {
        UUID orgId = TenantContext.getTenantId();
        ClientEntity client = clientRepository.findByIdAndOrganizationId(clientId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: '" + clientId + "'"));

        ComplianceObligationEntity obligation = obligationRepository.findByIdAndOrganizationId(obligationId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance obligation not found with id: '" + obligationId + "'"));

        if (!obligation.getClientId().equals(clientId)) {
            throw new ResourceNotFoundException("Compliance obligation not found for client id: '" + clientId + "'");
        }

        if (obligation.getStatus() == ComplianceObligationStatus.COMPLETED || obligation.getStatus() == ComplianceObligationStatus.CANCELLED) {
            throw new BadRequestException("Cannot recalculate due date for obligation in terminal state: " + obligation.getStatus());
        }

        DueDateCalculationResult result = calculateDueDateForObligation(obligation);
        LocalDate previousDate = obligation.getStatutoryDueDate();

        if (result.getStatus() == DueDateCalculationStatus.CALCULATED) {
            obligation.setStatutoryDueDate(result.getStatutoryDueDate());
            obligation.setDueDate(result.getDueDate());
        }

        obligation.setDueDateCalculationStatus(result.getStatus());
        obligation.setDueDateExplanation(result.getExplanation());
        obligation.setDueDateCalculatedAt(Instant.now());
        obligation.setDueDateRuleType(result.getStrategy());

        ComplianceObligationEntity saved = obligationRepository.save(obligation);

        // Audit recalculation if date changed or was newly established
        if (previousDate == null || !previousDate.equals(saved.getStatutoryDueDate())) {
            auditService.logEvent(
                    "COMPLIANCE_OBLIGATION_DUE_DATE_RECALCULATED",
                    "COMPLIANCE_OBLIGATION",
                    saved.getId().toString(),
                    null,
                    "Recalculated statutory due date for " + saved.getTitle() + ": " + saved.getStatutoryDueDate() + " (" + result.getExplanation() + ")"
            );
        }

        log.info("Recalculated due date for obligation id={}, client={}, date={}, status={}",
                obligationId, clientId, saved.getStatutoryDueDate(), result.getStatus());

        return mapToDto(saved, client);
    }

    @Override
    public DueDateCalculationResult previewDueDate(String ruleCode, CompliancePeriodType periodType, String periodKey) {
        if (ruleCode == null || ruleCode.trim().isEmpty()) {
            throw new BadRequestException("Rule code is required for due-date preview.");
        }
        if (periodType == null || periodKey == null) {
            throw new BadRequestException("Period type and key are required for due-date preview.");
        }

        UUID orgId = TenantContext.getTenantId();
        Optional<ComplianceRuleEntity> ruleOpt = ruleRepository.findByRuleCodeAndOrganizationIdIsNull(ruleCode.trim());
        if (orgId != null) {
            Optional<ComplianceRuleEntity> tenantRuleOpt = ruleRepository.findByRuleCodeAndOrganizationId(ruleCode.trim(), orgId);
            if (tenantRuleOpt.isPresent()) {
                ruleOpt = tenantRuleOpt;
            }
        }

        if (ruleOpt.isEmpty()) {
            throw new ResourceNotFoundException("Rule not found in catalog: '" + ruleCode + "'");
        }

        CompliancePeriod period = periodService.resolvePeriod(periodType, periodKey);
        return calculateDueDate(ruleOpt.get(), period, LocalDate.now());
    }

    private ComplianceObligationDto mapToDto(ComplianceObligationEntity entity, ClientEntity client) {
        return ComplianceObligationDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .clientId(entity.getClientId())
                .clientName(client != null ? client.getDisplayName() : null)
                .clientDisplayName(client != null ? client.getDisplayName() : null)
                .pan(client != null ? client.getPan() : null)
                .ruleId(entity.getRuleId())
                .ruleCode(entity.getRuleCode())
                .ruleVersion(entity.getRuleVersion())
                .ruleNameSnapshot(entity.getRuleNameSnapshot())
                .domain(entity.getDomain())
                .periodType(entity.getPeriodType())
                .periodKey(entity.getPeriodKey())
                .applicabilityState(entity.getApplicabilityState())
                .applicabilityReason(entity.getApplicabilityReason())
                .generatedAt(entity.getGeneratedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancelledBy(entity.getCancelledBy())
                .cancellationReason(entity.getCancellationReason())
                .title(entity.getTitle() != null ? entity.getTitle() : (entity.getRuleNameSnapshot() != null ? entity.getRuleNameSnapshot() : "Compliance Obligation"))
                .description(entity.getDescription())
                .obligationType(entity.getObligationType())
                .periodLabel(entity.getPeriodLabel() != null ? entity.getPeriodLabel() : entity.getPeriodKey())
                .financialYear(entity.getFinancialYear())
                .assessmentYear(entity.getAssessmentYear())
                .statutoryDueDate(entity.getStatutoryDueDate())
                .internalTargetDate(entity.getInternalTargetDate())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .dueDate(entity.getDueDate() != null ? entity.getDueDate() : entity.getStatutoryDueDate())
                .period(entity.getPeriod() != null ? entity.getPeriod() : entity.getPeriodKey())
                .complianceType(entity.getComplianceType() != null ? entity.getComplianceType().name() : (entity.getDomain() != null ? entity.getDomain().name() : null))
                .dueDateCalculationStatus(entity.getDueDateCalculationStatus() != null ? entity.getDueDateCalculationStatus().name() : null)
                .dueDateExplanation(entity.getDueDateExplanation())
                .dueDateCalculatedAt(entity.getDueDateCalculatedAt())
                .dueDateRuleType(entity.getDueDateRuleType() != null ? entity.getDueDateRuleType().name() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
