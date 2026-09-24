package com.taxoryn.module.compliance.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationType;
import com.taxoryn.module.task.entity.TaskEntity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Compliance Calendar Search & Filter Parameters")
public class ComplianceCalendarFilterRequest extends PageRequestDto {

    @Schema(description = "Search term across title, client name, notes")
    private String search;

    @Schema(description = "Filter by Client ID")
    private UUID clientId;

    @Schema(description = "Filter by Client Service ID")
    private UUID clientServiceId;

    @Schema(description = "Filter by Service Period ID")
    private UUID servicePeriodId;

    @Schema(description = "Filter by Obligation Type")
    private ComplianceObligationType obligationType;

    @Schema(description = "Filter by Period Label (e.g. August 2026, Q2 FY 2026-27)")
    private String periodLabel;

    @Schema(description = "Filter by Financial Year (e.g. 2026-27)")
    private String financialYear;

    @Schema(description = "Filter by Assessment Year (e.g. 2027-28)")
    private String assessmentYear;

    @Schema(description = "Filter by Obligation Status")
    private ComplianceObligationStatus status;

    @Schema(description = "Filter by Priority")
    private TaskPriority priority;

    @Schema(description = "Filter by assigned employee ID")
    private UUID assignedEmployeeId;

    @Schema(description = "Filter due dates from this start date (inclusive)", example = "2026-08-01")
    private LocalDate startDate;

    @Schema(description = "Filter due dates up to this end date (inclusive)", example = "2026-08-31")
    private LocalDate endDate;

    @Schema(description = "Filter for obligations due today")
    private Boolean isDueToday;

    @Schema(description = "Filter for obligations due this week")
    private Boolean isDueThisWeek;

    @Schema(description = "Filter for overdue obligations (due before today and not completed)")
    private Boolean isOverdue;

    @Schema(description = "Filter for obligations waiting for client action")
    private Boolean isWaitingForClient;

    @Schema(description = "Filter for obligations ready for filing")
    private Boolean isReadyForFiling;

    @Schema(description = "Filter only obligations assigned to current practitioner")
    private Boolean myObligationsOnly;

    // Legacy compatibility getter
    public String getPeriod() {
        return periodLabel;
    }
}
