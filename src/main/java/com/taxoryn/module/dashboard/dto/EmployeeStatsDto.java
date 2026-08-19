package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization Employee Statistics")
public class EmployeeStatsDto {

    @Schema(description = "Total number of employees in organization", example = "25")
    private long total;

    @Schema(description = "Number of active employees", example = "22")
    private long active;
}
