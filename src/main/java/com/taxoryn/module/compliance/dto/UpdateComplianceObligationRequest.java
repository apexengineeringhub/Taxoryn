package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
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
@Schema(description = "Update Compliance Obligation Payload")
public class UpdateComplianceObligationRequest {

    private String title;
    private String description;
    private ComplianceObligationType obligationType;
    private String periodLabel;
    private String financialYear;
    private String assessmentYear;
    private LocalDate statutoryDueDate;
    private LocalDate internalTargetDate;
    private TaskPriority priority;
    private UUID assignedEmployeeId;
    private String notes;
}
