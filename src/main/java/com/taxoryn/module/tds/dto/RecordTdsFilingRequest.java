package com.taxoryn.module.tds.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to record final filing details and token number for a TDS return")
public class RecordTdsFilingRequest {

    @NotNull(message = "Filing date is required")
    @Schema(description = "Date return was submitted", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate filingDate;

    @NotBlank(message = "Token Number / PRN is required")
    @Schema(description = "15-digit Provisional Receipt Number / Token Number", example = "010020304050607", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tokenNumber;

    @Schema(description = "Receipt Reference or Acknowledgement Number")
    private String receiptNumber;

    @Schema(description = "Practitioner Notes")
    private String notes;
}
