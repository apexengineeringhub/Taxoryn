package com.taxoryn.module.compliance.profile.dto;

import com.taxoryn.module.compliance.profile.model.ComplianceFilingFrequency;
import com.taxoryn.module.compliance.profile.model.ComplianceTdsDeductorCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "TDS Compliance Configuration Facts")
public class TdsComplianceConfigDto {

    @Schema(description = "Whether TDS/TCS compliance applies to this client", example = "true")
    private boolean applicable;

    @Schema(description = "TDS return filing frequency")
    private ComplianceFilingFrequency filingFrequency;

    @Schema(description = "TDS Deductor entity category")
    private ComplianceTdsDeductorCategory deductorCategory;

    @Schema(description = "Whether a lower/nil deduction certificate exists under Sec 197")
    private boolean lowerDeductionCertificate;
}
