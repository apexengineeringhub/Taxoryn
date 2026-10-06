package com.taxoryn.module.compliance.profile.dto;

import com.taxoryn.module.compliance.profile.model.ComplianceFilingFrequency;
import com.taxoryn.module.compliance.profile.model.ComplianceGstRegistrationType;
import com.taxoryn.module.compliance.profile.model.ComplianceItrCategory;
import com.taxoryn.module.compliance.profile.model.ComplianceProfileStatus;
import com.taxoryn.module.compliance.profile.model.ComplianceTdsDeductorCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to create or update a Client Compliance Profile (Phase 29.1)")
public class UpdateComplianceProfileRequest {

    @Schema(description = "Target profile status")
    private ComplianceProfileStatus status;

    // --- GST Configuration ---
    @Schema(description = "Whether GST applies", example = "true")
    private Boolean gstApplicable;

    @Schema(description = "GST registration category")
    private ComplianceGstRegistrationType gstRegistrationType;

    @Schema(description = "GST filing frequency")
    private ComplianceFilingFrequency gstFilingFrequency;

    @Schema(description = "Whether composition scheme applies")
    private Boolean gstCompositionScheme;

    @Schema(description = "Whether e-invoicing applies")
    private Boolean gstEinvoiceApplicable;

    @Schema(description = "Whether e-way bill applies")
    private Boolean gstEwaybillApplicable;

    // --- TDS Configuration ---
    @Schema(description = "Whether TDS applies", example = "true")
    private Boolean tdsApplicable;

    @Schema(description = "TDS filing frequency")
    private ComplianceFilingFrequency tdsFilingFrequency;

    @Schema(description = "TDS deductor entity category")
    private ComplianceTdsDeductorCategory tdsDeductorCategory;

    @Schema(description = "Whether lower deduction certificate applies")
    private Boolean tdsLowerDeductionCertificate;

    // --- ITR Configuration ---
    @Schema(description = "Whether ITR applies", example = "true")
    private Boolean itrApplicable;

    @Schema(description = "ITR return categorization")
    private ComplianceItrCategory itrCategory;

    @Schema(description = "Whether tax audit applies")
    private Boolean itrTaxAuditApplicable;

    @Schema(description = "Whether transfer pricing applies")
    private Boolean itrTransferPricingApplicable;

    // --- Other Statutory Configuration ---
    @Schema(description = "Whether advance tax applies")
    private Boolean advanceTaxApplicable;

    @Schema(description = "Whether MCA annual filings apply")
    private Boolean mcaFilingApplicable;

    @Schema(description = "Whether professional tax applies")
    private Boolean professionalTaxApplicable;

    @Schema(description = "Whether PF/ESI applies")
    private Boolean pfEsiApplicable;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    @Schema(description = "General compliance notes")
    private String notes;
}
