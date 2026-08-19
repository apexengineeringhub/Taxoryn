package com.taxoryn.module.compliance.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.compliance.entity.ComplianceObligationEntity.ComplianceStatus;
import com.taxoryn.module.compliance.entity.ComplianceRuleEntity.ComplianceType;
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

    @Schema(description = "Filter by Compliance Type (GST, ITR, TDS, OTHER, ROC, ADVANCE_TAX)")
    private ComplianceType complianceType;

    @Schema(description = "Filter by Period (e.g. 2026-08, 2026-Q2, 2026-27)")
    private String period;

    @Schema(description = "Filter by workflow status")
    private ComplianceStatus status;

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
}
