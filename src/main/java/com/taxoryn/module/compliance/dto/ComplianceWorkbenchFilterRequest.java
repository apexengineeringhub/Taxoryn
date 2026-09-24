package com.taxoryn.module.compliance.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.compliance.model.ComplianceWorkflowStatus;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Workbench Filter Parameters")
public class ComplianceWorkbenchFilterRequest extends PageRequestDto {

    public enum WorkbenchTab {
        MY_WORK,
        ALL,
        DUE_TODAY,
        DUE_THIS_WEEK,
        OVERDUE,
        WAITING_FOR_CLIENT,
        UNDER_REVIEW,
        READY_FOR_FILING,
        COMPLETED
    }

    @Schema(description = "Active workbench view tab", example = "MY_WORK")
    private WorkbenchTab tab;

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Client Service ID")
    private UUID clientServiceId;

    @Schema(description = "Filter by Obligation Type", example = "GST_RETURN")
    private ComplianceObligationType obligationType;

    @Schema(description = "Filter by Workflow Status", example = "IN_PROGRESS")
    private ComplianceWorkflowStatus status;

    @Schema(description = "Filter by Priority level", example = "HIGH")
    private TaskPriority priority;

    @Schema(description = "Filter by Assigned Practitioner Employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Filter by Reviewer Employee ID")
    private UUID reviewerEmployeeId;

    @Schema(description = "Filter by Waiting for Client flag")
    private Boolean waitingForClient;

    @Schema(description = "Quick view type / preset", example = "MY_ASSIGNED")
    private String viewType;

    @Schema(description = "Search query against client name, pan, gstin, or obligation title")
    private String search;

    @Schema(description = "Start statutory due date filter (YYYY-MM-DD)", example = "2026-08-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;

    @Schema(description = "End statutory due date filter (YYYY-MM-DD)", example = "2026-08-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    @Schema(description = "Start target date filter (YYYY-MM-DD)", example = "2026-08-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetFrom;

    @Schema(description = "End target date filter (YYYY-MM-DD)", example = "2026-08-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetTo;
}
