package com.taxoryn.module.compliance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Finalize Compliance Workflow Completion")
public class CompleteComplianceWorkflowRequest {

    @Schema(description = "Final Government Acknowledgement Number / ARN / Challan Reference", example = "AA2708260192837")
    private String acknowledgementNumber;

    @Schema(description = "Final closing remarks")
    private String notes;
}
