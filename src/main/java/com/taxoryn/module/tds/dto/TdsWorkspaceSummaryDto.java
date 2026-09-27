package com.taxoryn.module.tds.dto;

import com.taxoryn.module.client.dto.ClientDto;
import com.taxoryn.module.compliance.dto.ComplianceObligationDto;
import com.taxoryn.module.compliance.dto.ComplianceWorkflowDto;
import com.taxoryn.module.docrequest.dto.DocumentRequestDto;
import com.taxoryn.module.document.dto.DocumentDto;
import com.taxoryn.module.task.dto.TaskDto;
import com.taxoryn.module.task.dto.WorkItemDto;
import com.taxoryn.module.tds.entity.TdsProfileEntity.DeductorType;
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
@Schema(description = "Consolidated TDS Compliance Workspace Summary for Client")
public class TdsWorkspaceSummaryDto {

    @Schema(description = "Client Master Details")
    private ClientDto client;

    @Schema(description = "Primary TDS/TAN Deductor Profile")
    private TdsProfileDto profile;

    @Schema(description = "Client TAN Number", example = "BLRP12345A")
    private String tan;

    @Schema(description = "Deductor Constitution Type", example = "COMPANY")
    private DeductorType deductorType;

    @Schema(description = "Active / Default Financial Year", example = "2026-27")
    private String defaultFinancialYear;

    @Schema(description = "Active / Default Quarter", example = "Q1")
    private String defaultQuarter;

    @Schema(description = "Available Compliance Periods")
    private List<TdsPeriodDto> periods;

    @Schema(description = "Active Statutory TDS Obligations (Quarterly Statements & Challans)")
    private List<ComplianceObligationDto> activeObligations;

    @Schema(description = "Active In-Progress TDS Workflows")
    private List<ComplianceWorkflowDto> currentWorkflows;

    @Schema(description = "Overdue TDS Workflows")
    private List<ComplianceWorkflowDto> overdueWorkflows;

    @Schema(description = "Upcoming Statutory TDS Due Dates")
    private List<ComplianceObligationDto> upcomingDueDates;

    @Schema(description = "Linked Operational Work Items")
    private List<WorkItemDto> linkedWorkItems;

    @Schema(description = "Pending Operational Tasks")
    private List<TaskDto> pendingTasks;

    @Schema(description = "Pending Document Requests to Client")
    private List<DocumentRequestDto> pendingDocumentRequests;

    @Schema(description = "Submitted Supporting Documents in Vault")
    private List<DocumentDto> submittedDocuments;

    @Schema(description = "Recorded Time Tracking Entries")
    private List<TimeEntryDto> timeEntries;

    @Schema(description = "Quarterly TDS Return Statements")
    private List<TdsReturnDto> recentReturns;

    @Schema(description = "Recent ITNS 281 Challan Deposits")
    private List<TdsChallanDto> recentChallans;
}
