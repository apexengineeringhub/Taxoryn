package com.taxoryn.module.gst.dto;

import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.timetracking.dto.TimeEntryDto;
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
@Schema(description = "Consolidated GST Compliance Workspace Aggregation View for a Client")
public class GstWorkspaceSummaryDto {

    @Schema(description = "Client master information")
    private ClientDto client;

    @Schema(description = "All GST registrations active for this client")
    private List<GstRegistrationDto> registrations;

    @Schema(description = "Active GST compliance obligations (GSTR-1, GSTR-3B, GSTR-9, etc.)")
    private List<ComplianceObligationDto> activeObligations;

    @Schema(description = "Current in-progress GST compliance workflows")
    private List<ComplianceWorkflowDto> currentWorkflows;

    @Schema(description = "Overdue GST compliance workflows")
    private List<ComplianceWorkflowDto> overdueWorkflows;

    @Schema(description = "Upcoming statutory compliance obligations and deadlines")
    private List<ComplianceObligationDto> upcomingDueDates;

    @Schema(description = "Operational work items linked to GST compliance")
    private List<WorkItemDto> linkedWorkItems;

    @Schema(description = "Pending actionable tasks under GST workflows or work items")
    private List<TaskDto> pendingTasks;

    @Schema(description = "Pending document requests for GST returns")
    private List<DocumentRequestDto> pendingDocumentRequests;

    @Schema(description = "Submitted documents for GST returns (e.g. Sales registers, Purchase 2B/3B, Challans)")
    private List<DocumentDto> submittedDocuments;

    @Schema(description = "Time tracking entries recorded against GST client compliance")
    private List<TimeEntryDto> timeEntries;
}
