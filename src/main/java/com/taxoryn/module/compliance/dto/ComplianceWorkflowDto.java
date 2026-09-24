package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
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
@Schema(description = "Compliance Workflow Card & Overview Details")
public class ComplianceWorkflowDto {

    @Schema(description = "Workflow ID")
    private UUID id;

    @Schema(description = "Organization ID")
    private UUID organizationId;

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Client display name", example = "Alpha Traders Ltd")
    private String clientName;

    @Schema(description = "Client PAN", example = "AAACB1234D")
    private String pan;

    @Schema(description = "Client GSTIN", example = "27AAACB1234D1Z5")
    private String gstin;

    @Schema(description = "Associated Client Service ID")
    private UUID clientServiceId;

    @Schema(description = "Associated Client Service Name", example = "GST Compliance & Periodic Filings")
    private String serviceName;

    @Schema(description = "Linked Compliance Obligation ID")
    private UUID complianceObligationId;

    @Schema(description = "Obligation Title", example = "GSTR-3B Monthly Return & Tax Payment for July 2026")
    private String obligationTitle;

    @Schema(description = "Obligation Type", example = "GST_RETURN")
    private ComplianceObligationType obligationType;

    @Schema(description = "Period label", example = "July 2026")
    private String periodLabel;

    @Schema(description = "Statutory government filing deadline", example = "2026-08-20")
    private LocalDate statutoryDueDate;

    @Schema(description = "Practice internal target date", example = "2026-08-17")
    private LocalDate targetDate;

    @Schema(description = "Statutory Obligation Status", example = "IN_PROGRESS")
    private ComplianceObligationStatus statutoryStatus;

    @Schema(description = "Operational Workflow Status", example = "UNDER_REVIEW")
    private ComplianceWorkflowStatus workflowStatus;

    @Schema(description = "Priority level", example = "HIGH")
    private TaskPriority priority;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner name", example = "Rahul Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Assigned reviewer/checker employee ID")
    private UUID reviewerEmployeeId;

    @Schema(description = "Assigned reviewer/checker name", example = "CA Vikram Sharma")
    private String reviewerEmployeeName;

    @Schema(description = "Whether workflow is blocked waiting for client", example = "false")
    private boolean waitingForClient;

    @Schema(description = "Reason execution is blocked waiting for client")
    private String waitingReason;

    @Schema(description = "Expected client response date", example = "2026-08-15")
    private LocalDate expectedResponseDate;

    @Schema(description = "Total checklist steps", example = "9")
    private int totalChecklistSteps;

    @Schema(description = "Completed checklist steps", example = "5")
    private int completedChecklistSteps;

    @Schema(description = "Checklist completion percentage", example = "55")
    private int progressPercentage;

    @Schema(description = "Days remaining until statutory due date", example = "4")
    private long daysRemaining;

    @Schema(description = "Days remaining until internal target date", example = "1")
    private long targetDaysRemaining;

    @Schema(description = "Whether statutory due date has elapsed", example = "false")
    private boolean isOverdue;

    @Schema(description = "Government filing date", example = "2026-08-19")
    private LocalDate filedDate;

    @Schema(description = "Government acknowledgement / ARN number", example = "AA2708260192837")
    private String acknowledgementNumber;

    @Schema(description = "Workflow started timestamp")
    private Instant startedAt;

    @Schema(description = "Workflow submitted for review timestamp")
    private Instant submittedAt;

    @Schema(description = "Workflow approved timestamp")
    private Instant approvedAt;

    @Schema(description = "Workflow completion timestamp")
    private Instant completedAt;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
