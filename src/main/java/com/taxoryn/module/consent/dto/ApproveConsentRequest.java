package com.taxoryn.module.consent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to approve a pending taxpayer consent")
public class ApproveConsentRequest {

    @Schema(description = "User ID of consenting taxpayer (defaults to authenticated user)")
    private UUID consentingUserId;

    @Schema(description = "Optional approval notes or internal comments")
    private String notes;
}
