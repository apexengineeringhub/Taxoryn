package com.taxoryn.module.compliance.profile.dto;

import com.taxoryn.module.compliance.profile.model.ComplianceProfileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lightweight cross-domain read-model summary of Compliance Profile")
public class ComplianceProfileSummaryDto {

    @Schema(description = "Unique compliance profile ID")
    private UUID profileId;

    @Schema(description = "Associated client ID")
    private UUID clientId;

    @Schema(description = "Profile lifecycle status")
    private ComplianceProfileStatus status;

    @Schema(description = "Whether GST compliance is active/applicable")
    private boolean gstApplicable;

    @Schema(description = "GST registration category")
    private String gstRegistrationType;

    @Schema(description = "GST filing frequency")
    private String gstFilingFrequency;

    @Schema(description = "Whether TDS compliance is active/applicable")
    private boolean tdsApplicable;

    @Schema(description = "TDS filing frequency")
    private String tdsFilingFrequency;

    @Schema(description = "Whether ITR compliance is active/applicable")
    private boolean itrApplicable;

    @Schema(description = "ITR entity category")
    private String itrCategory;

    @Schema(description = "Readiness percentage score")
    private int readinessScore;
}
