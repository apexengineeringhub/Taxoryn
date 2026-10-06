package com.taxoryn.module.compliance.duedate.service;

import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.duedate.dto.DueDateCalculationResult;
import com.taxoryn.module.compliance.duedate.dto.ObligationDueDateDto;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity;
import com.taxoryn.module.compliance.period.model.CompliancePeriod;
import com.taxoryn.module.compliance.rule.entity.ComplianceRuleEntity;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;

import java.time.LocalDate;
import java.util.UUID;

public interface ComplianceDueDateService {

    /**
     * Deterministically calculates statutory due date for a given rule and compliance period.
     *
     * @param rule     The compliance rule definition with due-date strategy metadata.
     * @param period   The normalized compliance period.
     * @param asOfDate Calculation reference date.
     * @return Result containing calculated due date, strategy, and calculation trace.
     */
    DueDateCalculationResult calculateDueDate(ComplianceRuleEntity rule, CompliancePeriod period, LocalDate asOfDate);

    /**
     * Calculates statutory due date for an existing compliance obligation entity.
     *
     * @param obligation The compliance obligation entity.
     * @return Result containing calculated due date, strategy, and calculation trace.
     */
    DueDateCalculationResult calculateDueDateForObligation(ComplianceObligationEntity obligation);

    /**
     * Fetches due-date details and explanation for a specific obligation.
     */
    ObligationDueDateDto getObligationDueDate(UUID clientId, UUID obligationId);

    /**
     * Explicitly recalculates and persists the authoritative statutory due date for an obligation.
     *
     * @param clientId     The client UUID.
     * @param obligationId The obligation UUID.
     * @return Updated ComplianceObligationDto.
     */
    ComplianceObligationDto recalculateObligationDueDate(UUID clientId, UUID obligationId);

    /**
     * Previews statutory due date calculation for a rule code, period type, and period key without persistence.
     */
    DueDateCalculationResult previewDueDate(String ruleCode, CompliancePeriodType periodType, String periodKey);
}
