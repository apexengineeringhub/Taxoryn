package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Create ITR Return Filing Record Payload")
public class CreateItrReturnRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "Client ID", example = "d1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID clientId;

    @NotBlank(message = "Assessment year is required")
    @Schema(description = "Assessment Year (e.g. 2026-27)", example = "2026-27")
    private String assessmentYear;

    @NotBlank(message = "Financial year is required")
    @Schema(description = "Financial Year (e.g. 2025-26)", example = "2025-26")
    private String financialYear;

    @NotNull(message = "ITR Type is required")
    @Schema(description = "ITR Form Type (ITR_1 to ITR_7)", example = "ITR_1")
    private ItrType itrType;

    @Schema(description = "Taxpayer Type (optional, defaults from profile or client type)")
    private TaxpayerType taxpayerType;

    @Schema(description = "Statutory due date for filing", example = "2026-07-31")
    private LocalDate dueDate;

    @Schema(description = "Initial workflow status", defaultValue = "DOCUMENTS_PENDING")
    private ItrStatus status;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Notes or remarks")
    private String notes;
}
