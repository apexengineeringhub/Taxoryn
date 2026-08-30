package com.taxoryn.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Central Client Health, Activity, and Follow-up Report")
public class ClientReportDto {

    @Schema(description = "Total registered clients")
    private long totalClients;

    @Schema(description = "Active clients")
    private long activeClients;

    @Schema(description = "Inactive clients")
    private long inactiveClients;

    @Schema(description = "Clients with open/pending tasks or filings")
    private long clientsWithPendingWork;

    @Schema(description = "Clients with overdue tasks or obligations")
    private long clientsWithOverdueWork;

    @Schema(description = "Clients with pending document requests")
    private long clientsWithPendingDocs;

    // Follow-up Summary
    @Schema(description = "Total pending client follow-up actions")
    private long pendingClientActions;

    @Schema(description = "Client requests/actions due today")
    private long clientActionsDueToday;

    @Schema(description = "Overdue client actions")
    private long clientActionsOverdue;

    // Document Requests Pipeline
    @Schema(description = "Total document requests")
    private long totalDocRequests;

    @Schema(description = "Requests awaiting client upload")
    private long docRequestsAwaitingUpload;

    @Schema(description = "Requests with uploaded files pending practitioner review")
    private long docRequestsUploaded;

    @Schema(description = "Requests accepted / completed")
    private long docRequestsAccepted;

    @Schema(description = "Requests rejected / requiring re-upload")
    private long docRequestsRejected;

    @Schema(description = "Detailed list of top clients requiring attention")
    private List<ClientAttentionItemDto> clientsRequiringAttention;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Client requiring practitioner attention due to overdue or pending items")
    public static class ClientAttentionItemDto {
        private java.util.UUID clientId;
        private String displayName;
        private String pan;
        private String clientType;
        private String assignedStaffName;
        private long openTasks;
        private long overdueTasks;
        private long pendingDocRequests;
        private boolean hasOverdueCompliance;
    }
}
