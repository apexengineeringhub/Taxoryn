package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsCertificateEntity.CertificateType;
import com.taxoryn.module.tds.entity.TdsCertificateEntity.DispatchStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to generate or register a Form 16 / 16A / 27D certificate")
public class CreateTdsCertificateRequest {

    @NotNull(message = "TDS Profile ID is required")
    @Schema(description = "TDS Profile ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID tdsProfileId;

    @Schema(description = "TDS Return ID")
    private UUID tdsReturnId;

    @Builder.Default
    @Schema(description = "Certificate Type", example = "FORM_16A")
    private CertificateType certificateType = CertificateType.FORM_16A;

    @NotBlank(message = "Financial Year is required")
    @Schema(description = "Financial Year", example = "2026-27", requiredMode = Schema.RequiredMode.REQUIRED)
    private String financialYear;

    @Schema(description = "Quarter (for Form 16A / 27D)")
    private TdsQuarter quarter;

    @NotBlank(message = "Deductee PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid 10-character PAN format")
    @Schema(description = "Deductee PAN", example = "ABCPS1234F", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deducteePan;

    @NotBlank(message = "Deductee Name is required")
    @Schema(description = "Deductee Name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deducteeName;

    @Schema(description = "TRACES Request Number")
    private String tracesRequestNumber;

    @Schema(description = "Certificate Serial Number")
    private String certificateNumber;

    @Schema(description = "Generation Date")
    private LocalDate generationDate;

    @Builder.Default
    @Schema(description = "Dispatch Status", example = "PENDING")
    private DispatchStatus dispatchStatus = DispatchStatus.PENDING;

    @Schema(description = "Notes")
    private String notes;
}
