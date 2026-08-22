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
}
