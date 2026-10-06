package com.taxoryn.module.compliance.obligation.dto;

import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.rule.model.CompliancePeriodType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing generated and existing compliance obligations for a client and period")
public class GeneratedObligationsResponseDto {

    @Schema(description = "Client ID")
    private UUID clientId;

    @Schema(description = "Period type evaluated", example = "MONTH")
    private CompliancePeriodType periodType;

    @Schema(description = "Period key evaluated", example = "2026-09")
    private String periodKey;

    @Schema(description = "Total number of applicable obligations returned", example = "3")
    private int totalCount;

    @Schema(description = "Number of newly generated obligations", example = "2")
    private int createdCount;

    @Schema(description = "Number of already existing obligations returned idempotently", example = "1")
    private int existingCount;

    @Schema(description = "List of generated or existing compliance obligations")
    private List<ComplianceObligationDto> obligations;
}
