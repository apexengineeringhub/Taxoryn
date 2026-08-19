package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.CompliancePriority;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Create Custom Compliance Obligation Payload")
public class CreateComplianceObligationRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client ID to associate compliance obligation with")
    private UUID clientId;

    @Schema(description = "Optional Rule ID if instantiated from a rule")
    private UUID ruleId;

    @NotBlank(message = "Title is required")
    @Schema(description = "Obligation title", example = "TDS Quarterly Return Q2 26Q")
    private String title;

    @NotNull(message = "Compliance type is required")
    @Schema(description = "Compliance domain type", example = "TDS")
    private ComplianceType complianceType;

    @NotBlank(message = "Period is required")
    @Schema(description = "Period (e.g. 2026-08, 2026-Q2, 2026-27)", example = "2026-Q2")
    private String period;

    @NotNull(message = "Due date is required")
    @Schema(description = "Statutory due date", example = "2026-10-31")
    private LocalDate dueDate;

    @Schema(description = "Initial status", defaultValue = "PENDING")
    private ComplianceStatus status;

    @Schema(description = "Priority level", defaultValue = "MEDIUM")
    private CompliancePriority priority;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Notes or remarks")
    private String notes;
}
