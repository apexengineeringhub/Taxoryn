package com.taxoryn.module.compliance.duedate.model;

/**
 * Deterministic status outcome of statutory due-date calculation.
 */
public enum DueDateCalculationStatus {
    /**
     * Authoritative statutory due date successfully calculated from rule metadata and period.
     */
    CALCULATED,

    /**
     * Rule does not define adequate due-date metadata (no date guessed).
     */
    NOT_CONFIGURED,

    /**
     * Rule due-date metadata contains invalid parameters (e.g. invalid day/month).
     */
    INVALID_CONFIGURATION,

    /**
     * Compliance period falls outside the rule's statutory validity window (effectiveFrom / effectiveTo).
     */
    OUT_OF_EFFECTIVE_RANGE,

    /**
     * Compliance rule has no statutory due date requirement (e.g. ad-hoc / event without deadline).
     */
    NOT_APPLICABLE
}
