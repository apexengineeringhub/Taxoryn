package com.taxoryn.module.reminder.entity;

/**
 * Recurrence pattern for operational reminders.
 *
 * Note: Statutory compliance recurrence is handled by the WorkTemplate system.
 * This recurrence is for operational reminders only — e.g. "Every Monday remind me
 * to review pending client documents."
 */
public enum ReminderRecurrenceType {
    /** One-time reminder (default). */
    NONE,
    /** Repeats every day. */
    DAILY,
    /** Repeats every week. */
    WEEKLY,
    /** Repeats every month. */
    MONTHLY
}
