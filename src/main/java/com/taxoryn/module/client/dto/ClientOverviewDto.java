package com.taxoryn.module.client.dto;

import com.taxoryn.module.task.dto.TaskDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    @Schema(description = "Active client service engagements")
    private List<ClientServiceItem> services;

    @Schema(description = "Client Workflow Tasks Analytics")
    private ClientTaskSummary taskSummary;

    @Schema(description = "Compliance & Filing Summary (GST, ITR, TDS, Accounting)")
    private ClientComplianceSummary complianceSummary;

    @Schema(description = "Documents Vault Summary")
    private ClientDocumentSummary documentsSummary;

    @Schema(description = "Client Document Requests Summary")
    private ClientDocRequestSummary docRequestsSummary;

    @Schema(description = "Billing & Financial Summary (Redacted for non-billing staff)")
    private ClientBillingSummary billingSummary;

    @Schema(description = "Tax Notices & Litigation Summary")
    private ClientNoticeSummary noticeSummary;

    @Schema(description = "Recent Communication History & Interaction Notes")
    private List<ClientNoteDto> recentNotes;

    @Schema(description = "Chronological Unified Activity Timeline")
    private List<ClientActivityItem> activityTimeline;

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
    public static class ClientServiceItem {
        private String serviceCode;
        private String serviceName;
        private String status;
        private String identifier;
        private String summary;
        private String routePath;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientTaskSummary {
        private long totalTasks;
        private long pendingTasks;
        private long inProgressTasks;
        private long underReviewTasks;
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
        private GstComplianceDetails gstDetails;
        private ItrComplianceDetails itrDetails;
        private TdsComplianceDetails tdsDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstComplianceDetails {
        private boolean registered;
        private String gstin;
        private String filingFrequency;
        private long totalFilings;
        private long pendingFilings;
        private long filedFilings;
        private long overdueFilings;
        private LocalDate nextDueDate;
        private String nextReturnType;
        private String nextReturnPeriod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItrComplianceDetails {
        private boolean registered;
        private String pan;
        private String taxpayerType;
        private String defaultItrType;
        private long totalReturns;
        private long pendingReturns;
        private long filedReturns;
        private long overdueReturns;
        private LocalDate nextDueDate;
        private String currentAssessmentYear;
        private String currentStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdsComplianceDetails {
        private boolean registered;
        private String tan;
        private String deductorType;
        private long totalReturns;
        private long pendingReturns;
        private long filedReturns;
        private long overdueReturns;
        private LocalDate nextDueDate;
        private String currentQuarter;
        private String currentFinancialYear;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientDocumentSummary {
        private long totalDocuments;
        private List<String> documentCategories;
        private List<ClientDocumentItem> recentDocuments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientDocumentItem {
        private UUID id;
        private String fileName;
        private String documentCategory;
        private Long fileSize;
        private String fileType;
        private Instant uploadedAt;
        private String fileUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientDocRequestSummary {
        private long totalRequests;
        private long pendingRequests;
        private long receivedRequests;
        private long overdueRequests;
        private List<ClientDocRequestItem> recentRequests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientDocRequestItem {
        private UUID id;
        private String requestNumber;
        private String title;
        private String status;
        private String priority;
        private LocalDate dueDate;
        private int totalItems;
        private int receivedItems;
        private Instant createdAt;
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
        private long totalInvoicesCount;
        private long overdueInvoicesCount;
        private List<ClientInvoiceItem> recentInvoices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInvoiceItem {
        private UUID id;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private LocalDate dueDate;
        private BigDecimal total;
        private BigDecimal paidAmount;
        private BigDecimal balanceDue;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientNoticeSummary {
        private long totalNotices;
        private long activeNotices;
        private long overdueNotices;
        private long hearingsScheduled;
        private BigDecimal totalDemandAmount;
        private List<ClientNoticeItem> recentNotices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientNoticeItem {
        private UUID id;
        private String noticeNumber;
        private String issuingAuthority;
        private String section;
        private String taxPeriod;
        private String status;
        private BigDecimal demandAmount;
        private LocalDate responseDueDate;
        private LocalDate hearingDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientActivityItem {
        private String id;
        private String eventType;
        private String title;
        private String description;
        private String performedBy;
        private Instant timestamp;
        private String category;
    }
}
