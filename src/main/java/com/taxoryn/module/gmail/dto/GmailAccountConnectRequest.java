package com.taxoryn.module.gmail.dto;

import com.taxoryn.module.gmail.entity.GmailAccountType;
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
@Schema(description = "Request payload to connect a Gmail mailbox via Google OAuth authorization code")
public class GmailAccountConnectRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(description = "Google OAuth2 authorization code returned from OAuth consent screen", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authCode;

    @Schema(description = "Redirect URI matching the initial OAuth consent request")
    private String redirectUri;

    @Schema(description = "Account type: PRACTICE_SHARED (firm-wide) or INDIVIDUAL_PRACTITIONER")
    @Builder.Default
    private GmailAccountType accountType = GmailAccountType.PRACTICE_SHARED;
}
