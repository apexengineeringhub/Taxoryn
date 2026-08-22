package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer acceptance or rejection of a practitioner's proposal")
public class AcceptProposalRequest {

    @Builder.Default
    @Schema(description = "Accept or Reject")
    private Boolean isAccepted = true;

    private String rejectionReason;

    private String clientNotes;
}
