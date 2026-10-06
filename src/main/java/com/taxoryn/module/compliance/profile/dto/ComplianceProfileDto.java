package com.taxoryn.module.compliance.profile.dto;

import com.taxoryn.module.client.dto.ClientContextSummaryDto;
import com.taxoryn.module.compliance.profile.model.ComplianceProfileStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full Compliance Profile Representation (Phase 29.1)")
public class ComplianceProfileDto {

    @Schema(description = "Unique compliance profile identifier")
    private UUID id;

    @Schema(description = "Owning organization / tenant identifier")
    private UUID organizationId;

    @Schema(description = "Associated client identifier")
    private UUID clientId;

    @Schema(description = "Profile lifecycle status")
    private ComplianceProfileStatus status;

    @Schema(description = "Authoritative client summary (resolved via application contract from Client domain)")
    private ClientContextSummaryDto clientSummary;

    @Schema(description = "GST compliance configuration facts")
    private GstComplianceConfigDto gstConfig;

    @Schema(description = "TDS compliance configuration facts")
    private TdsComplianceConfigDto tdsConfig;

    @Schema(description = "ITR compliance configuration facts")
    private ItrComplianceConfigDto itrConfig;

    @Schema(description = "Other statutory compliance configuration facts")
    private OtherComplianceConfigDto otherComplianceConfig;

    @Schema(description = "General compliance operational notes")
    private String notes;

    @Schema(description = "Readiness and completeness assessment")
    private ComplianceProfileCompletenessDto completeness;

    @Schema(description = "Record creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "Created by user/system ID")
    private String createdBy;

    @Schema(description = "Last updated by user/system ID")
    private String updatedBy;

    @Schema(description = "Optimistic locking version number")
    private Long version;
}
