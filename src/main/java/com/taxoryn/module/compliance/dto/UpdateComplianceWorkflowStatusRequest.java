package com.taxoryn.module.compliance.dto;

import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to update compliance workflow status")
public class UpdateComplianceWorkflowStatusRequest {

    @NotNull(message = "Workflow status is required")
    @Schema(description = "Target operational compliance workflow status")
    private ComplianceWorkflowStatus status;

    @Schema(description = "Optional reason or contextual notes for the status change")
    private String reason;

    @Schema(description = "Filing date (if transitioning to FILED)")
    private LocalDate filedDate;

    @Schema(description = "Government portal filing acknowledgement or ARN number (if transitioning to FILED)")
    private String acknowledgementNumber;
}
