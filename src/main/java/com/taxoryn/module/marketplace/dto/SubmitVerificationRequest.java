package com.taxoryn.module.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request submitted by a Practice to verify their official credentials")
public class SubmitVerificationRequest {

    @NotBlank(message = "Professional body is required")
    @Schema(description = "Governing Statutory Body", example = "ICAI")
    private String professionalBody;

    @NotBlank(message = "Membership number is required")
    @Schema(description = "Fellow / Associate Membership Number", example = "FCA-504932")
    private String membershipNumber;

    @Schema(description = "Certificate of Practice (COP) Number", example = "COP-2018/984")
    private String copNumber;

    @Schema(description = "Firm Registration Number (FRN)", example = "104928W")
    private String firmRegistrationNumber;

    @Schema(description = "Uploaded COP or Membership Certificate Document URL")
    private String documentUrl;
}
