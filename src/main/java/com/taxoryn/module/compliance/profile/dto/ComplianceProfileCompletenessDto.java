package com.taxoryn.module.compliance.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deterministic readiness and completeness evaluation of the Compliance Profile")
public class ComplianceProfileCompletenessDto {

    @Schema(description = "Whether the compliance profile has been initialized with configured facts")
    private boolean configured;

    @Schema(description = "Whether GST configuration has been evaluated/configured")
    private boolean gstConfigured;

    @Schema(description = "Whether TDS configuration has been evaluated/configured")
    private boolean tdsConfigured;

    @Schema(description = "Whether ITR configuration has been evaluated/configured")
    private boolean itrConfigured;

    @Schema(description = "Readiness percentage score (0 to 100%)", example = "100")
    private int readinessScore;

    @Schema(description = "Readiness summary label", example = "READY")
    private String readinessSummary;

    @Schema(description = "List of pending configuration recommendations for applicability engine readiness")
    @Builder.Default
    private List<String> pendingItems = new ArrayList<>();
}
