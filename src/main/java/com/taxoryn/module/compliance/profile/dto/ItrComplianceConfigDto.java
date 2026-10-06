package com.taxoryn.module.compliance.profile.dto;

import com.taxoryn.module.compliance.profile.model.ComplianceItrCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Income Tax (ITR) Compliance Configuration Facts")
public class ItrComplianceConfigDto {

    @Schema(description = "Whether ITR compliance applies to this client", example = "true")
    private boolean applicable;

    @Schema(description = "ITR return categorization")
    private ComplianceItrCategory category;

    @Schema(description = "Whether Tax Audit under Section 44AB applies")
    private boolean taxAuditApplicable;

    @Schema(description = "Whether Transfer Pricing / Form 3CEB applies")
    private boolean transferPricingApplicable;
}
