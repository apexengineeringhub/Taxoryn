package com.taxoryn.module.dashboard.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dashboard Time Period & Filter Resolution Unit Tests")
class DashboardTimePeriodTest {

    private final StringToDashboardTimePeriodConverter converter = new StringToDashboardTimePeriodConverter();

    @Test
    @DisplayName("DashboardTimePeriod: fromString parses standard, lowercase, hyphenated and unknown values")
    void testFromStringParsing() {
        assertEquals(DashboardTimePeriod.LAST_MONTH, DashboardTimePeriod.fromString("LAST_MONTH"));
        assertEquals(DashboardTimePeriod.LAST_MONTH, DashboardTimePeriod.fromString("last_month"));
        assertEquals(DashboardTimePeriod.LAST_MONTH, DashboardTimePeriod.fromString("last-month"));
        assertEquals(DashboardTimePeriod.LAST_MONTH, DashboardTimePeriod.fromString("Last Month"));

        assertEquals(DashboardTimePeriod.TODAY, DashboardTimePeriod.fromString("today"));
        assertEquals(DashboardTimePeriod.THIS_WEEK, DashboardTimePeriod.fromString("this_week"));
        assertEquals(DashboardTimePeriod.LAST_WEEK, DashboardTimePeriod.fromString("last-week"));
        assertEquals(DashboardTimePeriod.THIS_QUARTER, DashboardTimePeriod.fromString("this_quarter"));
        assertEquals(DashboardTimePeriod.LAST_QUARTER, DashboardTimePeriod.fromString("last_quarter"));
        assertEquals(DashboardTimePeriod.THIS_YEAR, DashboardTimePeriod.fromString("this_year"));
        assertEquals(DashboardTimePeriod.LAST_YEAR, DashboardTimePeriod.fromString("last_year"));

        assertEquals(DashboardTimePeriod.ALL_TIME, DashboardTimePeriod.fromString(null));
        assertEquals(DashboardTimePeriod.ALL_TIME, DashboardTimePeriod.fromString(""));
        assertEquals(DashboardTimePeriod.ALL_TIME, DashboardTimePeriod.fromString("UNKNOWN_VALUE"));
    }

    @Test
    @DisplayName("StringToDashboardTimePeriodConverter: converts query parameter value accurately")
    void testConverter() {
        assertEquals(DashboardTimePeriod.LAST_MONTH, converter.convert("LAST_MONTH"));
        assertEquals(DashboardTimePeriod.THIS_MONTH, converter.convert("THIS_MONTH"));
        assertEquals(DashboardTimePeriod.ALL_TIME, converter.convert("INVALID_PERIOD"));
    }

    @Test
    @DisplayName("DashboardFilterRequest: resolves LAST_MONTH start and end dates correctly")
    void testResolveLastMonthDates() {
        DashboardFilterRequest request = DashboardFilterRequest.builder()
                .period(DashboardTimePeriod.LAST_MONTH)
                .build();

        LocalDate today = LocalDate.now();
        LocalDate expectedStart = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        LocalDate expectedEnd = today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        assertEquals(expectedStart, request.resolveStartDate());
        assertEquals(expectedEnd, request.resolveEndDate());
        assertTrue(request.resolveStartDate().isBefore(request.resolveEndDate()) || request.resolveStartDate().isEqual(request.resolveEndDate()));
        assertEquals(1, request.resolveStartDate().getDayOfMonth());
    }

    @Test
    @DisplayName("DashboardFilterRequest: resolves LAST_WEEK start and end dates correctly")
    void testResolveLastWeekDates() {
        DashboardFilterRequest request = DashboardFilterRequest.builder()
                .period(DashboardTimePeriod.LAST_WEEK)
                .build();

        LocalDate today = LocalDate.now();
        LocalDate expectedStart = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate expectedEnd = today.minusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        assertEquals(expectedStart, request.resolveStartDate());
        assertEquals(expectedEnd, request.resolveEndDate());
    }

    @Test
    @DisplayName("DashboardFilterRequest: resolves LAST_QUARTER start and end dates correctly")
    void testResolveLastQuarterDates() {
        DashboardFilterRequest request = DashboardFilterRequest.builder()
                .period(DashboardTimePeriod.LAST_QUARTER)
                .build();

        LocalDate today = LocalDate.now();
        int currentQuarterFirstMonth = ((today.getMonthValue() - 1) / 3) * 3 + 1;
        LocalDate firstDayOfThisQuarter = LocalDate.of(today.getYear(), currentQuarterFirstMonth, 1);
        LocalDate expectedStart = firstDayOfThisQuarter.minusMonths(3);
        LocalDate expectedEnd = firstDayOfThisQuarter.minusDays(1);

        assertEquals(expectedStart, request.resolveStartDate());
        assertEquals(expectedEnd, request.resolveEndDate());
        assertTrue(request.resolveStartDate().isBefore(request.resolveEndDate()));
    }

    @Test
    @DisplayName("DashboardFilterRequest: resolves LAST_YEAR start and end dates correctly")
    void testResolveLastYearDates() {
        DashboardFilterRequest request = DashboardFilterRequest.builder()
                .period(DashboardTimePeriod.LAST_YEAR)
                .build();

        LocalDate today = LocalDate.now();
        LocalDate expectedStart = LocalDate.of(today.getYear() - 1, 1, 1);
        LocalDate expectedEnd = LocalDate.of(today.getYear() - 1, 12, 31);

        assertEquals(expectedStart, request.resolveStartDate());
        assertEquals(expectedEnd, request.resolveEndDate());
    }
}
