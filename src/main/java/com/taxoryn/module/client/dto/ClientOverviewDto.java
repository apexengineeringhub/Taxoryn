package com.taxoryn.module.client.dto;

import com.taxoryn.module.task.dto.TaskDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "360-Degree Unified Client Overview & Practice Dashboard")
public class ClientOverviewDto {

    @Schema(description = "Client master details & contact information")
    private ClientDto client;

    @Schema(description = "Statutory & Tax Registrations")
    private StatutoryDetails statutory;

    @Schema(description = "Client Workflow Tasks Analytics")
    private ClientTaskSummary taskSummary;

    @Schema(description = "Compliance & Filing Summary (GST, ITR, TDS, Accounting)")
    private ClientComplianceSummary complianceSummary;

    @Schema(description = "Documents Vault Summary")
    private ClientDocumentSummary documentsSummary;

    @Schema(description = "Billing & Financial Summary")
    private ClientBillingSummary billingSummary;

    @Schema(description = "Recent Communication History & Interaction Notes")
    private List<ClientNoteDto> recentNotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatutoryDetails {
        private String pan;
        private String gstin;
        private String tan;
        private String cin;
        private LocalDate dateOfIncorporation;
        @com.fasterxml.jackson.annotation.JsonProperty("isPanValid")
        private boolean isPanValid;
        @com.fasterxml.jackson.annotation.JsonProperty("isGstActive")
        private boolean isGstActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientTaskSummary {
        private long totalTasks;
        private long pendingTasks;
        private long overdueTasks;
        private long completedTasks;
        private List<TaskDto> recentTasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientComplianceSummary {
        private String gstStatus;
        private String itrStatus;
        private String tdsStatus;
        private String accountingStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientDocumentSummary {
        private long totalDocuments;
        private List<String> documentCategories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientBillingSummary {
        private double totalInvoiced;
        private double totalPaid;
        private double outstandingBalance;
        private String currency;
    }
}
