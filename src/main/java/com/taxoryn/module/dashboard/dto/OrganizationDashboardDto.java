package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization-Level Dashboard Summary")
public class OrganizationDashboardDto {

    @Schema(description = "Client statistics summary")
    private ClientStatsDto clients;

    @Schema(description = "Employee statistics summary")
    private EmployeeStatsDto employees;

    @Schema(description = "Task statistics summary")
    private TaskStatsDto tasks;

    @Schema(description = "GST compliance statistics summary")
    private GstStatsDto gst;

    @Schema(description = "ITR compliance statistics summary")
    private ItrStatsDto itr;

    @Schema(description = "Billing and revenue statistics summary")
    private BillingStatsDto billing;

    @Schema(description = "Employee workload distribution")
    private List<EmployeeWorkloadItemDto> employeeWorkload;
}
