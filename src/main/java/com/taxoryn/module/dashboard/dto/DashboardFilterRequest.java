package com.taxoryn.module.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard query filter payload")
public class DashboardFilterRequest {

    @Schema(description = "Reporting time period (TODAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH, THIS_QUARTER, LAST_QUARTER, THIS_YEAR, LAST_YEAR, CUSTOM, ALL_TIME)", example = "THIS_MONTH")
    @Builder.Default
    private DashboardTimePeriod period = DashboardTimePeriod.ALL_TIME;

    @Schema(description = "Custom start date (used when period is CUSTOM)")
    private LocalDate startDate;

    @Schema(description = "Custom end date (used when period is CUSTOM)")
    private LocalDate endDate;

    @Schema(description = "Filter by specific practice location ID (validated against user's location access)")
    private UUID locationId;

    @Schema(description = "Filter by specific client ID (validated against user's client access)")
    private UUID clientId;

    public LocalDate resolveStartDate() {
        if (period == null || period == DashboardTimePeriod.ALL_TIME) {
            return startDate;
        }
        LocalDate today = LocalDate.now();
        return switch (period) {
            case TODAY -> today;
            case THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case LAST_WEEK -> today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case THIS_MONTH -> today.with(TemporalAdjusters.firstDayOfMonth());
            case LAST_MONTH -> today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            case THIS_QUARTER -> {
                int firstMonthOfQuarter = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                yield LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
            }
            case LAST_QUARTER -> {
                int currentQuarterFirstMonth = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate firstDayOfThisQuarter = LocalDate.of(today.getYear(), currentQuarterFirstMonth, 1);
                yield firstDayOfThisQuarter.minusMonths(3);
            }
            case THIS_YEAR -> LocalDate.of(today.getYear(), 1, 1);
            case LAST_YEAR -> LocalDate.of(today.getYear() - 1, 1, 1);
            case CUSTOM -> startDate;
            case ALL_TIME -> null;
        };
    }

    public LocalDate resolveEndDate() {
        if (period == null || period == DashboardTimePeriod.ALL_TIME) {
            return endDate;
        }
        LocalDate today = LocalDate.now();
        return switch (period) {
            case TODAY -> today;
            case THIS_WEEK -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case LAST_WEEK -> today.minusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case THIS_MONTH -> today.with(TemporalAdjusters.lastDayOfMonth());
            case LAST_MONTH -> today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
            case THIS_QUARTER -> {
                int firstMonthOfQuarter = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate firstDay = LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
                yield firstDay.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
            }
            case LAST_QUARTER -> {
                int currentQuarterFirstMonth = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate firstDayOfThisQuarter = LocalDate.of(today.getYear(), currentQuarterFirstMonth, 1);
                yield firstDayOfThisQuarter.minusDays(1);
            }
            case THIS_YEAR -> LocalDate.of(today.getYear(), 12, 31);
            case LAST_YEAR -> LocalDate.of(today.getYear() - 1, 12, 31);
            case CUSTOM -> endDate;
            case ALL_TIME -> null;
        };
    }
}
