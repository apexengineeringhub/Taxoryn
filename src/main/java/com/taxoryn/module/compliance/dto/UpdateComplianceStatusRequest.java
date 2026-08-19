package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
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
@Schema(description = "Update Compliance Obligation Status Payload")
public class UpdateComplianceStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target status", example = "COMPLETED")
    private ComplianceStatus status;

    @Schema(description = "Optional notes or resolution details")
    private String notes;
}
