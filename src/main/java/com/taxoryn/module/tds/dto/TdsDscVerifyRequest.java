package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify a TDS / TRACES DSC digital signature")
public class TdsDscVerifyRequest {

    @NotBlank(message = "Signature reference is required")
    @Schema(description = "Safe signature token or reference identifier")
    private String signatureReference;

    @Schema(description = "Safe certificate reference identifier")
    private String certificateReference;

    @Schema(description = "Action or directive reference")
    private String actionReference;

    @Schema(description = "Mock directives or provider options")
    private Map<String, Object> options;
}
