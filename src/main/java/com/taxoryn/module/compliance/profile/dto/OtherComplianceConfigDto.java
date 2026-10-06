package com.taxoryn.module.compliance.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Other Statutory & Regulatory Compliance Configuration Facts")
public class OtherComplianceConfigDto {

    @Schema(description = "Whether Advance Tax installments apply")
    private boolean advanceTaxApplicable;

    @Schema(description = "Whether MCA Annual ROC Filings apply (Companies / LLPs)")
    private boolean mcaFilingApplicable;

    @Schema(description = "Whether Professional Tax (PT) filings apply")
    private boolean professionalTaxApplicable;

    @Schema(description = "Whether Provident Fund & ESI monthly returns apply")
    private boolean pfEsiApplicable;
}
