package com.taxoryn.module.compliance.obligation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel an existing compliance obligation")
public class CancelObligationRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Schema(description = "Business reason for cancelling the obligation", example = "Client opted for composition scheme for this quarter")
    private String cancellationReason;
}
