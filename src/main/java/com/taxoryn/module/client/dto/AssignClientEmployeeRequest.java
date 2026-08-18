package com.taxoryn.module.client.dto;

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
@Schema(description = "Assign or Reassign Employee / Account Manager to Client")
public class AssignClientEmployeeRequest {

    @NotNull(message = "Assigned employee ID is required")
    @Schema(description = "Employee ID to assign", example = "c1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID employeeId;
}
