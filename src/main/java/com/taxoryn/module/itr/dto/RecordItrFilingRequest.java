package com.taxoryn.module.itr.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Record ITR e-Filing Submission Details Payload")
public class RecordItrFilingRequest {

    @Schema(description = "Date of e-filing submission", example = "2026-07-28")
    private LocalDate filingDate;

    @NotBlank(message = "Acknowledgement number is required")
    @Schema(description = "ITR e-Filing Acknowledgement / ITR-V Ack number", example = "123456789012345")
    private String acknowledgementNumber;

    @Schema(description = "Date of e-Verification / Aadhaar OTP verification", example = "2026-07-28")
    private LocalDate verificationDate;

    @Schema(description = "Filing remarks or notes")
    private String notes;
}
