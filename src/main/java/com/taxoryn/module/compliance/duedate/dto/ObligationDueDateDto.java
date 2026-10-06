package com.taxoryn.module.compliance.duedate.dto;

import com.taxoryn.module.compliance.duedate.model.DueDateCalculationStatus;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.DueDateRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObligationDueDateDto {

    private UUID obligationId;
    private UUID clientId;
    private String ruleCode;
    private Integer ruleVersion;
    private String ruleName;
    private CompliancePeriodType periodType;
    private String periodKey;
    private String periodLabel;
    private LocalDate statutoryDueDate;
    private LocalDate dueDate;
    private DueDateCalculationStatus calculationStatus;
    private DueDateRuleType strategy;
    private String explanation;
    private Instant calculatedAt;
}
