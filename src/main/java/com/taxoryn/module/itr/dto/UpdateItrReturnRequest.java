package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Update ITR Return Filing Record Payload")
public class UpdateItrReturnRequest {

    @NotNull(message = "ITR Type is required")
    @Schema(description = "ITR Form Type (ITR_1 to ITR_7)", example = "ITR_1")
    private ItrType itrType;

    @Schema(description = "Taxpayer Type", example = "INDIVIDUAL")
    private TaxpayerType taxpayerType;

    @Schema(description = "Statutory due date", example = "2026-07-31")
    private LocalDate dueDate;

    @Schema(description = "Workflow status")
    private ItrStatus status;

    @Schema(description = "Assigned practitioner employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Linked Task ID")
    private UUID taskId;

    @Schema(description = "Linked Compliance ID")
    private UUID complianceId;

    @Schema(description = "Linked Document Request ID")
    private UUID documentRequestId;

    @Schema(description = "Notes or remarks")
    private String notes;
}
