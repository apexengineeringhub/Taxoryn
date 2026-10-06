package com.taxoryn.module.gov.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for continuing or completing an interactive government authorization flow.
 * Note: Never contains raw plaintext credentials, passwords, tokens, or private keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to continue or complete an interactive government authorization flow")
public class GovAuthContinueRequest {

    @Schema(description = "Safe non-secret action reference or correlation identifier returned from previous step")
    private String actionReference;

    @Schema(description = "Optional execution options or mock directives")
    private Map<String, Object> options;
}
