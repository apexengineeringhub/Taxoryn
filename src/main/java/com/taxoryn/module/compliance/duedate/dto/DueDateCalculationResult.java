package com.taxoryn.module.compliance.duedate.dto;

import com.taxoryn.module.compliance.duedate.model.DueDateCalculationStatus;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Immutable calculation result containing the authoritative statutory due date and calculation trace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DueDateCalculationResult {

    private LocalDate statutoryDueDate;
    private LocalDate dueDate;
    private DueDateCalculationStatus status;
    private DueDateRuleType strategy;
    private String ruleCode;
    private Integer ruleVersion;
    private String explanation;
    private LocalDate calculationDate;
    private String calculationTrace;
    private Integer statutoryGraceDays;

    public static DueDateCalculationResult calculated(
            LocalDate statutoryDueDate,
            DueDateRuleType strategy,
            String ruleCode,
            Integer ruleVersion,
            String explanation,
            String calculationTrace,
            Integer statutoryGraceDays) {
        return DueDateCalculationResult.builder()
                .statutoryDueDate(statutoryDueDate)
                .dueDate(statutoryDueDate)
                .status(DueDateCalculationStatus.CALCULATED)
                .strategy(strategy)
                .ruleCode(ruleCode)
                .ruleVersion(ruleVersion)
                .explanation(explanation)
                .calculationDate(LocalDate.now())
                .calculationTrace(calculationTrace)
                .statutoryGraceDays(statutoryGraceDays != null ? statutoryGraceDays : 0)
                .build();
    }

    public static DueDateCalculationResult notConfigured(String ruleCode, Integer ruleVersion, String reason) {
        return DueDateCalculationResult.builder()
                .status(DueDateCalculationStatus.NOT_CONFIGURED)
                .ruleCode(ruleCode)
                .ruleVersion(ruleVersion)
                .explanation(reason != null ? reason : "Due date configuration is not available for this rule.")
                .calculationDate(LocalDate.now())
                .calculationTrace("Result: NOT_CONFIGURED. " + reason)
                .build();
    }

    public static DueDateCalculationResult invalidConfig(String ruleCode, Integer ruleVersion, String reason) {
        return DueDateCalculationResult.builder()
                .status(DueDateCalculationStatus.INVALID_CONFIGURATION)
                .ruleCode(ruleCode)
                .ruleVersion(ruleVersion)
                .explanation(reason)
                .calculationDate(LocalDate.now())
                .calculationTrace("Result: INVALID_CONFIGURATION. " + reason)
                .build();
    }

    public static DueDateCalculationResult outOfEffectiveRange(String ruleCode, Integer ruleVersion, String reason) {
        return DueDateCalculationResult.builder()
                .status(DueDateCalculationStatus.OUT_OF_EFFECTIVE_RANGE)
                .ruleCode(ruleCode)
                .ruleVersion(ruleVersion)
                .explanation(reason)
                .calculationDate(LocalDate.now())
                .calculationTrace("Result: OUT_OF_EFFECTIVE_RANGE. " + reason)
                .build();
    }
}
