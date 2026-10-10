package com.taxoryn.module.compliance.health.dto;

import com.taxoryn.module.compliance.health.model.ComplianceHealthStatus;
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
@Schema(description = "Comprehensive Client Compliance Health and Intelligence summary")
public class ClientComplianceHealthDto {

    @Schema(description = "Client unique identifier")
    private UUID clientId;

    @Schema(description = "Client display name")
    private String clientDisplayName;

    @Schema(description = "Deterministic compliance health score between 0 and 100")
    private int healthScore;

    @Schema(description = "Compliance health status classification")
    private ComplianceHealthStatus healthStatus;

    @Schema(description = "Contributing factors that determined the score and status")
    private List<String> contributingFactors;

    @Schema(description = "Total number of compliance obligations tracked")
    private long totalObligations;

    @Schema(description = "Number of active, non-terminal obligations")
    private long openObligations;

    @Schema(description = "Count of overdue obligations")
    private long overdueCount;

    @Schema(description = "Count of obligations due today")
    private long dueTodayCount;

    @Schema(description = "Count of obligations due within 3 days")
    private long dueWithin3DaysCount;

    @Schema(description = "Count of eligible obligations without generated work")
    private long workNotGeneratedCount;

    @Schema(description = "Count of obligations with unconfigured or missing due dates")
    private long missingDueDateCount;

    @Schema(description = "Count of rules requiring additional client data for applicability evaluation")
    private long insufficientDataCount;

    @Schema(description = "Total critical risk signals detected")
    private long criticalRiskCount;

    @Schema(description = "Total high risk signals detected")
    private long highRiskCount;

    @Schema(description = "All identified compliance risk signals")
    private List<ComplianceRiskSignalDto> risks;

    @Schema(description = "Top actionable recommendations prioritized for the practitioner (up to 5)")
    private List<ComplianceRecommendationDto> recommendations;

    @Schema(description = "Timestamp when intelligence was calculated")
    private Instant calculatedAt;

    @Schema(description = "Business reference date used for calculations")
    private LocalDate referenceDate;
}
