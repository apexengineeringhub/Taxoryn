package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
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

    @Schema(description = "Optional Client Service ID")
    private UUID clientServiceId;

    @Schema(description = "Optional Service Period ID")
    private UUID servicePeriodId;

    @Schema(description = "Optional Workflow ID")
    private UUID workflowId;

    @Schema(description = "Optional Rule ID")
    private UUID ruleId;

    @NotNull(message = "Obligation type is required")
    @Schema(description = "Obligation type", example = "GST_RETURN")
    private ComplianceObligationType obligationType;

    @NotBlank(message = "Title is required")
    @Schema(description = "Obligation title", example = "GSTR-3B Monthly Return - August 2026")
    private String title;

    @Schema(description = "Obligation description")
    private String description;

    @NotBlank(message = "Period label is required")
    @Schema(description = "Period label (e.g. August 2026, Q2 FY 2026-27)", example = "August 2026")
    private String periodLabel;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Assessment Year", example = "2027-28")
    private String assessmentYear;

    @NotNull(message = "Statutory due date is required")
    @Schema(description = "Official Statutory Due Date", example = "2026-09-20")
    private LocalDate statutoryDueDate;

    @Schema(description = "Internal Practice Target Date", example = "2026-09-17")
    private LocalDate internalTargetDate;

    @Schema(description = "Initial status", defaultValue = "UPCOMING")
    private ComplianceObligationStatus status;

    @Schema(description = "Priority level", defaultValue = "MEDIUM")
    private TaskPriority priority;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Notes or remarks")
    private String notes;

    // Legacy compatibility aliases
    public String getPeriod() {
        return periodLabel;
    }

    public LocalDate getDueDate() {
        return statutoryDueDate;
    }

    public static class CreateComplianceObligationRequestBuilder {
        public CreateComplianceObligationRequestBuilder complianceType(com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType type) {
            if (type != null) {
                this.obligationType = switch (type) {
                    case GST -> ComplianceObligationType.GST_RETURN;
                    case ITR -> ComplianceObligationType.ITR_FILING;
                    case TDS -> ComplianceObligationType.TDS_RETURN;
                    case ROC -> ComplianceObligationType.ROC_COMPLIANCE;
                    case ADVANCE_TAX -> ComplianceObligationType.ADVANCE_TAX;
                    default -> ComplianceObligationType.OTHER;
                };
            }
            return this;
        }

        public CreateComplianceObligationRequestBuilder period(String period) {
            this.periodLabel = period;
            return this;
        }

        public CreateComplianceObligationRequestBuilder dueDate(LocalDate dueDate) {
            this.statutoryDueDate = dueDate;
            return this;
        }

        public CreateComplianceObligationRequestBuilder remarks(String remarks) {
            this.notes = remarks;
            return this;
        }

        public CreateComplianceObligationRequestBuilder status(ComplianceObligationStatus status) {
            this.status = status;
            return this;
        }

        public CreateComplianceObligationRequestBuilder status(com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus legacyStatus) {
            if (legacyStatus != null) {
                this.status = switch (legacyStatus) {
                    case PENDING -> ComplianceObligationStatus.UPCOMING;
                    case IN_PROGRESS -> ComplianceObligationStatus.IN_PROGRESS;
                    case COMPLETED -> ComplianceObligationStatus.COMPLETED;
                    case OVERDUE -> ComplianceObligationStatus.OVERDUE;
                    case WAIVED, CANCELLED -> ComplianceObligationStatus.CANCELLED;
                };
            }
            return this;
        }

        public CreateComplianceObligationRequestBuilder priority(TaskPriority priority) {
            this.priority = priority;
            return this;
        }

        public CreateComplianceObligationRequestBuilder priority(com.taxoryn.module.compliance.entity.ComplianceObligationEntity.CompliancePriority legacyPriority) {
            if (legacyPriority != null) {
                this.priority = switch (legacyPriority) {
                    case LOW -> TaskPriority.LOW;
                    case MEDIUM -> TaskPriority.MEDIUM;
                    case HIGH -> TaskPriority.HIGH;
                    case CRITICAL -> TaskPriority.URGENT;
                };
            }
            return this;
        }
    }
}
