package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.entity.ComplianceWorkStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplianceWorkStatusRequest {

    @NotNull(message = "Target status is required")
    private ComplianceWorkStatus status;

    private String notes;
}
