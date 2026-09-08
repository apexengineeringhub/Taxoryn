package com.taxoryn.module.authentication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validation response for organization or employee activation token")
public class ValidateActivationTokenResponse {

    @Schema(description = "Whether the activation token is valid and unexpired")
    private boolean valid;

    @Schema(description = "Email of the invited user or administrator")
    private String email;

    @Schema(description = "Name of the organization/practice")
    private String organizationName;

    @Schema(description = "Full name of the user being activated")
    private String userFullName;

    @Schema(description = "Whether the user needs to set up a new password during activation")
    private boolean requiresPasswordSetup;
}
