package com.taxoryn.module.employee.dto;

import com.taxoryn.module.employee.entity.EmployeeEntity.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update Employee Employment Status Request")
public class UpdateEmployeeStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target employment status", example = "INACTIVE")
    private EmployeeStatus status;
}
