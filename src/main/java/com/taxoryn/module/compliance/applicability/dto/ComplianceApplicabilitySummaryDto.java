package com.taxoryn.module.compliance.applicability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Compact summary of a client's evaluated compliance applicability.
 * Used across Client 360, Business Context, and Overview dashboards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compact summary of client compliance rule applicability")
public class ComplianceApplicabilitySummaryDto {

    @Schema(description = "Client UUID")
    private UUID clientId;

    @Schema(description = "Evaluation date used for rule validity and effective date calculations")
    private LocalDate evaluationDate;

    @Schema(description = "Total number of active rules evaluated in the catalog", example = "31")
    private int totalEvaluatedRules;

    @Schema(description = "Number of rules determined to be APPLICABLE", example = "6")
    private int applicableCount;

    @Schema(description = "Number of rules determined to be NOT_APPLICABLE", example = "23")
    private int notApplicableCount;

    @Schema(description = "Number of rules requiring further client profile configuration (INSUFFICIENT_DATA)", example = "2")
    private int insufficientDataCount;

    @Schema(description = "Breakdown of applicable rules count by domain")
    private Map<String, Integer> applicableByDomain;

    @Schema(description = "Whether the client has a configured compliance profile")
    private boolean profileConfigured;

    @Schema(description = "Timestamp when this summary was computed")
    private Instant evaluatedAt;
}
