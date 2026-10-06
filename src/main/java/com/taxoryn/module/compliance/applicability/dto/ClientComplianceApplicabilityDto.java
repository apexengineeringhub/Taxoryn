package com.taxoryn.module.compliance.applicability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full applicability report response for a client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete compliance rule applicability report for a client")
public class ClientComplianceApplicabilityDto {

    @Schema(description = "Client UUID")
    private UUID clientId;

    @Schema(description = "Client Display Name")
    private String clientDisplayName;

    @Schema(description = "Client Legal / Constitution Type", example = "PRIVATE_LIMITED")
    private String clientType;

    @Schema(description = "Evaluation date used", example = "2026-10-06")
    private LocalDate evaluationDate;

    @Schema(description = "Evaluation summary metrics")
    private ComplianceApplicabilitySummaryDto summary;

    @Schema(description = "Detailed list of evaluated compliance rules")
    private List<EvaluatedRuleApplicabilityDto> rules;

    @Schema(description = "Timestamp when evaluation was executed")
    private Instant evaluatedAt;
}
