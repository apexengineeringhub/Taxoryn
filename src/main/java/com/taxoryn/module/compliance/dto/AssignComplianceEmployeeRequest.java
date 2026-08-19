package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assign Practitioner to Compliance Obligation Payload")
public class AssignComplianceEmployeeRequest {

    @NotNull(message = "Employee ID is required")
    @Schema(description = "Employee ID to assign")
    private UUID employeeId;
}
