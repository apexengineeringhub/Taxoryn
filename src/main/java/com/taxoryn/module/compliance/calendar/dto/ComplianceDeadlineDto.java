package com.taxoryn.module.compliance.calendar.dto;

import com.taxoryn.module.compliance.calendar.model.DeadlineStatus;
import com.taxoryn.module.compliance.duedate.model.DueDateCalculationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Projected Compliance Deadline Item for Calendar and Radar")
public class ComplianceDeadlineDto {

    @Schema(description = "Obligation unique identifier")
    private UUID obligationId;

    @Schema(description = "Client unique identifier")
    private UUID clientId;

    @Schema(description = "Client primary legal or commercial name")
    private String clientName;

    @Schema(description = "Client display name")
    private String clientDisplayName;

    @Schema(description = "Client PAN")
    private String clientPan;

    @Schema(description = "Statutory compliance rule code (e.g. GST_GSTR3B_MONTHLY)")
    private String ruleCode;

    @Schema(description = "Rule version snapshot")
    private Integer ruleVersion;

    @Schema(description = "Human-readable rule name snapshot")
    private String ruleName;

    @Schema(description = "Statutory compliance domain (e.g. GST, TDS, INCOME_TAX, MCA_ROC)")
    private ComplianceRuleDomain domain;

    @Schema(description = "Period frequency type (e.g. MONTH, QUARTER, FINANCIAL_YEAR)")
    private CompliancePeriodType periodType;

    @Schema(description = "Canonical period key (e.g. 2026-09, 2026-27-Q1, 2026-27)")
    private String periodKey;

    @Schema(description = "Formatted period display label")
    private String periodLabel;

    @Schema(description = "Authoritative statutory due date")
    private LocalDate statutoryDueDate;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Lifecycle status of the underlying obligation")
    private ComplianceObligationStatus obligationStatus;

    @Schema(description = "Deterministic deadline classification relative to reference date")
    private DeadlineStatus deadlineStatus;

    @Schema(description = "Number of calendar days remaining until deadline (0 if due today or overdue)")
    private long daysRemaining;

    @Schema(description = "Number of calendar days past the deadline (0 if not overdue)")
    private long daysOverdue;

    @Schema(description = "Priority level")
    private TaskPriority priority;

    @Schema(description = "Due date calculation status from Phase 29.5 engine")
    private DueDateCalculationStatus dueDateCalculationStatus;

    @Schema(description = "Natural language explanation of statutory due date calculation")
    private String dueDateExplanation;
}
