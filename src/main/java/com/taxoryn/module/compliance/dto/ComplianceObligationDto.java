package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.CompliancePriority;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
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

    @Schema(description = "Client PAN", example = "AAACB1234D")
    private String pan;

    @Schema(description = "Client GSTIN", example = "27AAACB1234D1Z5")
    private String gstin;

    @Schema(description = "Associated Rule ID")
    private UUID ruleId;

    @Schema(description = "Obligation title", example = "GSTR-3B Monthly Return & Tax Payment for 2026-08")
    private String title;

    @Schema(description = "Compliance domain category", example = "GST")
    private ComplianceType complianceType;

    @Schema(description = "Compliance Period (e.g. 2026-08, 2026-Q2, 2026-27)", example = "2026-08")
    private String period;

    @Schema(description = "Statutory due date", example = "2026-09-20")
    private LocalDate dueDate;

    @Schema(description = "Compliance status", example = "PENDING")
    private ComplianceStatus status;

    @Schema(description = "Priority level", example = "HIGH")
    private CompliancePriority priority;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Assigned practitioner full name", example = "Rahul Sharma")
    private String assignedEmployeeName;

    @Schema(description = "Linked task ID in Task Management module")
    private UUID taskId;

    @Schema(description = "Completion timestamp")
    private Instant completedAt;

    @Schema(description = "Completed by user email/name")
    private String completedBy;

    @Schema(description = "Notes and remarks")
    private String notes;

    @Schema(description = "Days remaining until due date (negative if overdue)", example = "5")
    private long daysRemaining;

    @Schema(description = "Flag indicating if obligation is overdue", example = "false")
    private boolean isOverdue;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;
}
