package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsCertificateEntity.CertificateType;
import com.taxoryn.module.tds.entity.TdsCertificateEntity.DispatchStatus;
import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsQuarter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Form 16 / 16A / 27D TDS Certificate Record")
public class TdsCertificateDto {

    @Schema(description = "Certificate ID")
    private UUID id;

    @Schema(description = "TDS Profile ID")
    private UUID tdsProfileId;

    @Schema(description = "TAN Number")
    private String tan;

    @Schema(description = "Client Name")
    private String clientName;

    @Schema(description = "TDS Return ID")
    private UUID tdsReturnId;

    @Schema(description = "Certificate Type", example = "FORM_16A")
    private CertificateType certificateType;

    @Schema(description = "Financial Year", example = "2026-27")
    private String financialYear;

    @Schema(description = "Quarter", example = "Q1")
    private TdsQuarter quarter;

    @Schema(description = "Deductee PAN", example = "ABCPS1234F")
    private String deducteePan;

    @Schema(description = "Deductee Name", example = "Infosys Technologies Ltd")
    private String deducteeName;

    @Schema(description = "TRACES Request Number")
    private String tracesRequestNumber;

    @Schema(description = "Certificate Serial Number")
    private String certificateNumber;

    @Schema(description = "Generation Date")
    private LocalDate generationDate;

    @Schema(description = "Dispatch Status", example = "PENDING")
    private DispatchStatus dispatchStatus;

    @Schema(description = "Dispatched At Timestamp")
    private Instant dispatchedAt;

    @Schema(description = "Notes")
    private String notes;

    @Schema(description = "Created Timestamp")
    private Instant createdAt;

    @Schema(description = "Updated Timestamp")
    private Instant updatedAt;
}
