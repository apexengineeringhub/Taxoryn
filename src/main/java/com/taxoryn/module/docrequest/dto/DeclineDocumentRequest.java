package com.taxoryn.module.docrequest.dto;

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
@Schema(description = "Payload for practitioner declining a client document request")
public class DeclineDocumentRequest {

    @NotBlank(message = "Decline reason is mandatory")
    @Schema(description = "Explanation for why request is declined", example = "ITR filing has not yet been processed by the IT department.")
    private String declineReason;
}
