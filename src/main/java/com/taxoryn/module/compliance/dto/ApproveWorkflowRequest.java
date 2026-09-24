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
@Schema(description = "Reviewer Approval for Compliance Workflow")
public class ApproveWorkflowRequest {

    @Schema(description = "Optional reviewer approval remarks", example = "Reviewed and verified against GSTR-2B ledger")
    private String approvalNotes;

    private String notes;

    public String getNotes() {
        return notes != null ? notes : approvalNotes;
    }
}
