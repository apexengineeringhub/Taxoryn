package com.taxoryn.module.compliance.calendar.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.compliance.calendar.model.DeadlineStatus;
import com.taxoryn.module.compliance.model.ComplianceObligationStatus;
import com.taxoryn.module.compliance.rule.model.ComplianceRuleDomain;
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
@Schema(description = "Query filter parameters for Compliance Calendar and Deadline Radar")
public class ComplianceCalendarQueryFilter extends PageRequestDto {

    @Schema(description = "Filter deadlines from this date (inclusive)", example = "2026-10-01")
    private LocalDate from;

    @Schema(description = "Filter deadlines up to this date (inclusive)", example = "2026-10-31")
    private LocalDate to;

    @Schema(description = "Filter by compliance domain (e.g. GST, TDS, INCOME_TAX, MCA_ROC)")
    private ComplianceRuleDomain domain;

    @Schema(description = "Filter by deadline classification (e.g. OVERDUE, DUE_TODAY, DUE_THIS_WEEK, UPCOMING)")
    private DeadlineStatus deadlineStatus;

    @Schema(description = "Filter by underlying obligation lifecycle status (e.g. UPCOMING, IN_PROGRESS, COMPLETED)")
    private ComplianceObligationStatus obligationStatus;

    @Schema(description = "Filter by client identifier")
    private UUID clientId;

    @Schema(description = "Filter by statutory compliance rule code (e.g. GST_GSTR3B_MONTHLY)")
    private String ruleCode;

    @Schema(description = "Free-text search across client name, rule code, and title")
    private String search;

    @Schema(description = "Optional simulation/reference date for deterministic testing and radar projection", example = "2026-10-06")
    private LocalDate referenceDate;

    @Schema(description = "Include completed and cancelled obligations in calendar (default: false for active radar)")
    private Boolean includeTerminal;
}
