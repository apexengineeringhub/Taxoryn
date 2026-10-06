package com.taxoryn.module.tds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify a TDS / TRACES EVC code")
public class TdsEvcVerifyRequest {

    @NotBlank(message = "Verification code is required")
    @Schema(description = "The 6-digit EVC / OTP value (Never persisted or logged)")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String verificationCode;

    @Schema(description = "Action or directive reference")
    private String actionReference;

    @Schema(description = "Mock directives or provider options")
    private Map<String, Object> options;
}
