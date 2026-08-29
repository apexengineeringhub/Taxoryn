package com.taxoryn.module.itr.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Create / Schedule ITR Return Filing Record Payload")
public class CreateItrReturnRequest {

    @Schema(description = "Client ID (optional if PAN / Profile ID is provided)", example = "d1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID clientId;

    @Schema(description = "ITR Profile ID (optional if PAN is provided)")
    private UUID itrProfileId;

    @JsonAlias({"clientPan", "panNumber"})
    @Schema(description = "Taxpayer PAN (used for auto-resolution)", example = "ABCPJ9876M")
    private String pan;

    @NotBlank(message = "Assessment year is required")
    @JsonAlias({"ay", "assessment_year"})
    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @NotBlank(message = "Financial year is required")
    @JsonAlias({"fy", "financial_year"})
    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @JsonAlias({"formType", "itrForm", "form"})
    @Schema(description = "ITR Form Type (ITR_1 to ITR_7)", example = "ITR_1")
    @Builder.Default
    private ItrType itrType = ItrType.ITR_1;

    @JsonAlias({"category", "constitution"})
    @Schema(description = "Taxpayer Type (optional, defaults from profile or client type)")
    private TaxpayerType taxpayerType;

    @JsonAlias({"due_date"})
    @Schema(description = "Statutory due date for filing", example = "2026-07-31")
    private LocalDate dueDate;

    @JsonAlias({"filing_date"})
    @Schema(description = "Date of return filing", example = "2026-07-28")
    private LocalDate filingDate;

    @JsonAlias({"ackNo", "ackNumber", "itrv", "acknowledgementNo", "ack_number"})
    @Schema(description = "ITR-V / e-Filing Acknowledgement Number", example = "123456789012345")
    private String acknowledgementNumber;

    @JsonAlias({"filingStatus", "state"})
    @Schema(description = "Initial workflow status", defaultValue = "DOCUMENTS_PENDING")
    @Builder.Default
    private ItrStatus status = ItrStatus.DOCUMENTS_PENDING;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Whether to auto-generate a practice workflow task for this return", example = "true")
    @Builder.Default
    private Boolean createTask = true;

    @Schema(description = "Existing Linked Document Request ID")
    private UUID documentRequestId;

    @Schema(description = "Existing Linked Compliance Obligation ID")
    private UUID complianceId;

    @Schema(description = "Existing Linked Task ID")
    private UUID taskId;

    @Schema(description = "Notes or remarks")
    private String notes;
}
