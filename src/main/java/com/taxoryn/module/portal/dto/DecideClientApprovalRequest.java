package com.taxoryn.module.portal.dto;

import com.taxoryn.module.portal.entity.ClientApprovalDecision;
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
@Schema(description = "Payload for client decision on an approval request")
public class DecideClientApprovalRequest {

    @NotNull(message = "Decision (APPROVE or REJECT) is required")
    private ClientApprovalDecision decision;

    @Schema(description = "Optional content version expected by the client; rejects if draft has been superseded")
    private String expectedContentVersion;

    @Schema(description = "Confirmation notes or sign-off remarks")
    private String notes;

    @Schema(description = "Required explanation when rejecting request")
    private String rejectionReason;
}
