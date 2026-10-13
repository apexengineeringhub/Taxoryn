package com.taxoryn.module.compliance.qc.dto;

import com.taxoryn.module.compliance.qc.model.ComplianceAuthorizationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to Authorize Filing of Approved Compliance Work")
public class AuthorizeFilingRequest {

    @NotNull(message = "Authorization method is required")
    @Schema(description = "Method used to obtain or record filing authorization", example = "CLIENT_CONSENT")
    private ComplianceAuthorizationMethod authorizationMethod;

    @Schema(description = "Reference identifier (e.g. email reference, DSC token ID, confirmation document #)", example = "EMAIL-CONF-20260818-091")
    private String authorizationReference;

    @Schema(description = "Notes or instructions accompanying filing authorization")
    private String authorizationNotes;

    @Schema(description = "Optional name or email of the authorizing person")
    private String authorizedBy;
}
