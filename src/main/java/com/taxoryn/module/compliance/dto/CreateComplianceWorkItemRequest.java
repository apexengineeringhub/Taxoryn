package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceWorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateComplianceWorkItemRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotNull(message = "Client Service ID is required")
    private UUID clientServiceId;

    @NotNull(message = "Work Type is required")
    private ComplianceWorkType workType;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;

    @Size(max = 50, message = "Financial year cannot exceed 50 characters")
    private String financialYear;

    @Size(max = 50, message = "Assessment year cannot exceed 50 characters")
    private String assessmentYear;

    @Size(max = 50, message = "Compliance period cannot exceed 50 characters")
    private String compliancePeriod;

    private LocalDate statutoryDueDate;
    private LocalDate internalTargetDate;
    private UUID assignedEmployeeId;
    private UUID reviewerEmployeeId;
}
