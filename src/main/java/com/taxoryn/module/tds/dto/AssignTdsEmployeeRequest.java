package com.taxoryn.module.tds.dto;

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
@Schema(description = "Request to assign an employee to a TDS return filing")
public class AssignTdsEmployeeRequest {

    @NotNull(message = "Employee ID is required")
    @Schema(description = "Assigned Employee ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID employeeId;
}
