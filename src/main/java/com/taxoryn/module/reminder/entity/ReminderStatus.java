package com.taxoryn.module.reminder.entity;

/**
 * Lifecycle status of a Reminder.
 *
 * OVERDUE is intentionally absent — it is a derived condition
 * computed from {@code scheduledAt} relative to the current time.
 */
public enum ReminderStatus {
    /** Created and waiting to be processed by the scheduler. */
    PENDING,
    /** Scheduler has processed this reminder and generated the notification. */
    TRIGGERED,
    /** The responsible user has explicitly marked this reminder as done. */
    COMPLETED,
    /** Reminder was explicitly cancelled (e.g. the underlying task was cancelled). */
    CANCELLED
}
