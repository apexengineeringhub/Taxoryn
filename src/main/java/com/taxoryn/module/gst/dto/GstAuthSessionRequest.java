package com.taxoryn.module.gst.dto;

import com.taxoryn.module.gov.auth.model.GovAuthMethod;
import com.taxoryn.module.gst.model.GstAuthenticationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request to initiate a purpose-scoped GST / E-Way / E-Invoice authentication session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate a GST / E-Way / E-Invoice authentication session")
public class GstAuthSessionRequest {

    @NotNull(message = "Connection ID is required")
    @Schema(description = "Government Connection ID configured for GST")
    private UUID connectionId;

    @NotNull(message = "Authentication purpose is required")
    @Builder.Default
    @Schema(description = "Target authentication purpose (GST, EWAY_BILL, E_INVOICE)")
    private GstAuthenticationPurpose purpose = GstAuthenticationPurpose.GST;

    @NotNull(message = "Authentication method is required")
    @Builder.Default
    @Schema(description = "Authentication method (OAUTH2, OTP, EVC, DSC)")
    private GovAuthMethod authMethod = GovAuthMethod.OAUTH2;

    @Schema(description = "Optional client correlation identifier for distributed tracing")
    private String correlationId;

    @Schema(description = "Optional provider-specific options or mock directives")
    private Map<String, Object> options;
}
