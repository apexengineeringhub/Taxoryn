package com.taxoryn.module.tds.dto;

import com.taxoryn.module.tds.entity.TdsReturnEntity.TdsFilingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to update TDS return workflow status")
public class UpdateTdsReturnStatusRequest {

    @NotNull(message = "Filing Status is required")
    @Schema(description = "Target status (PENDING, DRAFT, CHALLANS_ATTACHED, UNDER_REVIEW, READY_TO_FILE, FILED, OVERDUE, CANCELLED)", requiredMode = Schema.RequiredMode.REQUIRED)
    private TdsFilingStatus filingStatus;

    @Schema(description = "Filing Date (if status is FILED)")
    private LocalDate filingDate;

    @Schema(description = "Token Number / PRN (if status is FILED)")
    private String tokenNumber;

    @Schema(description = "Receipt Reference")
    private String receiptNumber;

    @Schema(description = "Practitioner Notes")
    private String notes;

    public UpdateTdsReturnStatusRequest(TdsFilingStatus filingStatus, String notes) {
        this.filingStatus = filingStatus;
        this.notes = notes;
    }

    @Schema(description = "Review comments or rework feedback if returning for changes")
    private String reviewComments;

    @Schema(description = "Linked Task ID")
    private java.util.UUID taskId;

    @Schema(description = "Linked Compliance ID")
    private java.util.UUID complianceId;

    @Schema(description = "Linked Document Request ID")
    private java.util.UUID documentRequestId;
}
