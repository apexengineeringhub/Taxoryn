package com.taxoryn.module.compliance.profile.dto;

import com.taxoryn.module.compliance.profile.model.ComplianceFilingFrequency;
import com.taxoryn.module.compliance.profile.model.ComplianceGstRegistrationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GST Compliance Configuration Facts")
public class GstComplianceConfigDto {

    @Schema(description = "Whether GST compliance applies to this client", example = "true")
    private boolean applicable;

    @Schema(description = "GST registration category")
    private ComplianceGstRegistrationType registrationType;

    @Schema(description = "GST return filing frequency")
    private ComplianceFilingFrequency filingFrequency;

    @Schema(description = "Whether client is enrolled under GST Composition Scheme")
    private boolean compositionScheme;

    @Schema(description = "Whether e-Invoicing under GST is mandatory for this client")
    private boolean einvoiceApplicable;

    @Schema(description = "Whether e-Way Bill compliance is applicable")
    private boolean ewaybillApplicable;
}
