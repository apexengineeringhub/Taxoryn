package com.taxoryn.module.worktemplate.model;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Standard recurrence frequencies supported by Taxoryn practice templates.
 */
public enum RecurrenceType {
    ONCE,
    MONTHLY,
    QUARTERLY,
    YEARLY;

    /**
     * Calculates the period end date based on period start date and recurrence frequency.
     */
    public LocalDate calculatePeriodEnd(LocalDate periodStart, int interval) {
        if (periodStart == null) {
            return null;
        }
        int effectiveInterval = Math.max(1, interval);
        return switch (this) {
            case ONCE -> periodStart;
            case MONTHLY -> periodStart.plusMonths(effectiveInterval - 1).with(TemporalAdjusters.lastDayOfMonth());
            case QUARTERLY -> periodStart.plusMonths(3L * effectiveInterval - 1).with(TemporalAdjusters.lastDayOfMonth());
            case YEARLY -> periodStart.plusYears(effectiveInterval - 1).with(TemporalAdjusters.lastDayOfYear());
        };
    }

    /**
     * Calculates the next period start date following a completed or previous period.
     */
    public LocalDate calculateNextPeriodStart(LocalDate currentPeriodStart, int interval) {
        if (currentPeriodStart == null) {
            return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        }
        int effectiveInterval = Math.max(1, interval);
        return switch (this) {
            case ONCE -> currentPeriodStart;
            case MONTHLY -> currentPeriodStart.plusMonths(effectiveInterval).with(TemporalAdjusters.firstDayOfMonth());
            case QUARTERLY -> currentPeriodStart.plusMonths(3L * effectiveInterval).with(TemporalAdjusters.firstDayOfMonth());
            case YEARLY -> currentPeriodStart.plusYears(effectiveInterval).with(TemporalAdjusters.firstDayOfYear());
        };
    }
}
