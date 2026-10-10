package com.taxoryn.module.compliance.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice-wide compliance health intelligence overview across clients")
public class PracticeComplianceHealthSummaryDto {

    @Schema(description = "Organization UUID")
    private UUID organizationId;

    @Schema(description = "Total clients evaluated")
    private long totalClients;

    @Schema(description = "Total active obligations across all clients")
    private long totalActiveObligations;

    @Schema(description = "Total overdue obligations across practice")
    private long totalOverdueObligations;

    @Schema(description = "Total obligations due today across practice")
    private long totalDueTodayObligations;

    @Schema(description = "Total obligations due within 3 days across practice")
    private long totalDueWithin3DaysObligations;

    @Schema(description = "Total obligations with work missing across practice")
    private long totalWorkNotGeneratedObligations;

    @Schema(description = "Count of clients in CRITICAL status")
    private long criticalClientsCount;

    @Schema(description = "Count of clients in AT_RISK status")
    private long atRiskClientsCount;

    @Schema(description = "Count of clients in GOOD status")
    private long goodClientsCount;

    @Schema(description = "Count of clients in EXCELLENT status")
    private long excellentClientsCount;

    @Schema(description = "Top at-risk clients requiring practitioner intervention")
    private List<ClientComplianceHealthDto> topAtRiskClients;

    @Schema(description = "Calculation timestamp")
    private Instant calculatedAt;

    @Schema(description = "Reference date used")
    private LocalDate referenceDate;
}
