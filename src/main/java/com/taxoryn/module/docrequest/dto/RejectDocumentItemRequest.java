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
@Schema(description = "Practitioner Document Rejection Payload")
public class RejectDocumentItemRequest {

    @NotBlank(message = "Rejection reason is required")
    @Schema(description = "Detailed reason why uploaded document was rejected", example = "Uploaded bank statement is incomplete. Please provide statements for all 12 months.")
    private String rejectionReason;
}