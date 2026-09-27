package com.taxoryn.module.itr.dto;

import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.itr.entity.ItrProfileEntity;
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
@Schema(description = "Consolidated ITR Compliance Workspace Aggregation View for a Client")
public class ItrWorkspaceSummaryDto {

    @Schema(description = "Client master information")
    private ClientDto client;

    @Schema(description = "Active ITR profile for this client")
    private ItrProfileDto profile;

    @Schema(description = "Permanent Account Number (PAN)")
    private String pan;

    @Schema(description = "Taxpayer entity type")
    private ItrProfileEntity.TaxpayerType taxpayerType;

    @Schema(description = "Residential status under Income Tax Act")
    private ItrProfileEntity.ResidentialStatus residentialStatus;

    @Schema(description = "Default Assessment Year")
    private String defaultAssessmentYear;

    @Schema(description = "Applicable return form")
    private ItrProfileEntity.ItrType applicableReturnType;

    @Schema(description = "Available distinct Assessment Years for this client")
    private List<String> assessmentYears;

    @Schema(description = "Active ITR compliance obligations (ITR-1 to ITR-7, Tax Audit, Advance Tax)")
    private List<ComplianceObligationDto> activeObligations;

    @Schema(description = "Current in-progress ITR compliance workflows")
    private List<ComplianceWorkflowDto> currentWorkflows;

    @Schema(description = "Overdue ITR compliance workflows")
    private List<ComplianceWorkflowDto> overdueWorkflows;

    @Schema(description = "Upcoming statutory compliance obligations and deadlines")
    private List<ComplianceObligationDto> upcomingDueDates;

    @Schema(description = "Operational work items linked to ITR compliance")
    private List<WorkItemDto> linkedWorkItems;

    @Schema(description = "Pending actionable tasks under ITR workflows or work items")
    private List<TaskDto> pendingTasks;

    @Schema(description = "Pending document requests for ITR returns (Form 16, AIS/TIS, 26AS, etc.)")
    private List<DocumentRequestDto> pendingDocumentRequests;

    @Schema(description = "Submitted documents in vault for ITR compliance")
    private List<DocumentDto> submittedDocuments;

    @Schema(description = "Time tracking entries recorded against ITR client compliance")
    private List<TimeEntryDto> timeEntries;
}
