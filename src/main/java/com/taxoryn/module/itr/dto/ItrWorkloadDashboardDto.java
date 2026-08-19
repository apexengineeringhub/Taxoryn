package com.taxoryn.module.itr.dto;

import com.taxoryn.module.itr.entity.ItrProfileEntity.ItrType;
import com.taxoryn.module.itr.entity.ItrProfileEntity.TaxpayerType;
import com.taxoryn.module.itr.entity.ItrReturnEntity.ItrStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ITR Practice & Employee Workload Dashboard")
public class ItrWorkloadDashboardDto {

    @Schema(description = "Assessment Year (AY)", example = "2026-27")
    private String assessmentYear;

    @Schema(description = "Total active ITR returns tracked", example = "60")
    private long totalReturns;

    @Schema(description = "Returns in DOCUMENTS_PENDING status", example = "15")
    private long documentsPendingCount;

    @Schema(description = "Returns in DATA_ENTRY status", example = "10")
    private long dataEntryCount;

    @Schema(description = "Returns in UNDER_REVIEW status", example = "8")
    private long underReviewCount;

    @Schema(description = "Returns in READY_TO_FILE status", example = "7")
    private long readyToFileCount;

    @Schema(description = "Returns in FILED status", example = "5")
    private long filedCount;

    @Schema(description = "Returns in VERIFICATION_PENDING status", example = "3")
    private long verificationPendingCount;

    @Schema(description = "Returns in COMPLETED status", example = "12")
    private long completedCount;

    @Schema(description = "Count of overdue ITR returns", example = "4")
    private long overdueCount;

    @Schema(description = "Count of upcoming ITR returns within due date", example = "44")
    private long upcomingCount;

    @Schema(description = "Individual ITR Return workload items")
    private List<ItrClientWorkloadItem> returns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Individual ITR Client Workload Card")
    public static class ItrClientWorkloadItem {

        @Schema(description = "ITR Return ID")
        private UUID returnId;

        @Schema(description = "Client ID")
        private UUID clientId;

        @Schema(description = "Client display name", example = "Anand Ramesh Joshi")
        private String clientName;

        @Schema(description = "Permanent Account Number (PAN)", example = "ABCPJ9876M")
        private String pan;

        @Schema(description = "Assessment Year", example = "2026-27")
        private String assessmentYear;

        @Schema(description = "ITR Form Type", example = "ITR_1")
        private ItrType itrType;

        @Schema(description = "Taxpayer Category", example = "INDIVIDUAL")
        private TaxpayerType taxpayerType;

        @Schema(description = "Statutory due date", example = "2026-07-31")
        private LocalDate dueDate;

        @Schema(description = "Current workflow status", example = "DATA_ENTRY")
        private ItrStatus status;

        @Schema(description = "Assigned practitioner employee ID")
        private UUID assignedEmployeeId;

        @Schema(description = "Assigned practitioner full name", example = "Vikram Sharma")
        private String assignedTo;

        @Schema(description = "Flag indicating whether return is currently overdue", example = "false")
        private boolean isOverdue;
    }
}
