package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Compliance Obligation Record Details")
public class ComplianceObligationDto {

    @Schema(description = "Obligation ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "ABC Traders")
    private String clientName;

    @Schema(description = "Client display name alias", example = "ABC Traders")
    private String clientDisplayName;

    @Schema(description = "Client PAN", example = "AAACB1234D")
    private String pan;

    @Schema(description = "Client GSTIN", example = "27AAACB1234D1Z5")
    private String gstin;

    @Schema(description = "Associated Client Service ID")
    private UUID clientServiceId;

    @Schema(description = "Client Service Name", example = "GST Compliance & Periodic Filings")
    private String serviceName;

    @Schema(description = "Associated Service Period ID")
    private UUID servicePeriodId;

    @Schema(description = "Associated Workflow ID")
    private UUID workflowId;

    @Schema(description = "Associated Workflow Title")
    private String workflowTitle;

    @Schema(description = "Associated Workflow Status")
    private String workflowStatus;

    @Schema(description = "Associated Rule ID")
    private UUID ruleId;

    @Schema(description = "Obligation Type", example = "GST_RETURN")
    private ComplianceObligationType obligationType;

    @Schema(description = "Obligation title", example = "GSTR-3B Monthly Return & Tax Payment for July 2026")
    private String title;

    @Schema(description = "Obligation description")
    private String description;

    @Schema(description = "Compliance Period Label (e.g. July 2026, Q1 FY 2026-27, AY 2026-27)", example = "July 2026")
    private String periodLabel;

    @Schema(description = "Financial Year (e.g. 2026-27)", example = "2026-27")
    private String financialYear;

    @Schema(description = "Assessment Year (e.g. 2027-28)", example = "2027-28")
    private String assessmentYear;

    @Schema(description = "Official Statutory Due Date", example = "2026-08-20")
    private LocalDate statutoryDueDate;

    @Schema(description = "Internal Practice Target Date", example = "2026-08-17")
    private LocalDate internalTargetDate;

    @Schema(description = "Compliance status", example = "IN_PROGRESS")
    private ComplianceObligationStatus status;

    @Schema(description = "Priority level", example = "HIGH")
    private TaskPriority priority;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner full name", example = "Rahul Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Assigned practitioner email")
    private String assignedEmployeeEmail;

    @Schema(description = "Linked task ID in Task Management module")
    private UUID taskId;

    @Schema(description = "Completion timestamp")
    private Instant completedAt;

    @Schema(description = "Completed by user email/name")
    private String completedBy;

    @Schema(description = "Government portal filing date", example = "2026-08-20")
    private LocalDate filedDate;

    @Schema(description = "Government portal filing timestamp")
    private Instant filedAt;

    @Schema(description = "Filed by user email/name")
    private String filedBy;

    @Schema(description = "Notes and remarks")
    private String notes;

    @Schema(description = "Days remaining until statutory due date (negative if overdue)", example = "5")
    private long daysRemaining;

    @Schema(description = "Flag indicating if obligation is overdue", example = "false")
    private boolean isOverdue;

    @Schema(description = "Legacy period string alias", example = "July 2026")
    private String period;

    @Schema(description = "Legacy due date alias", example = "2026-08-20")
    private LocalDate dueDate;

    @Schema(description = "Legacy compliance type alias", example = "GST")
    private String complianceType;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
