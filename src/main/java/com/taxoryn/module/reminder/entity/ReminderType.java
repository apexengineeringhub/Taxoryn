package com.taxoryn.module.reminder.entity;

/**
 * Classifier for what event triggered or should trigger this reminder.
 * Kept generic — no GST/TDS/ITR-specific types.
 * Tax-specific context flows through the entity relationships (taskId, workInstanceId, etc.)
 */
public enum ReminderType {
    /** Reminder linked to a task approaching its due date. */
    TASK_DUE,
    /** Reminder linked to a task that has already passed its due date. */
    TASK_OVERDUE,
    /** Manual follow-up reminder — e.g. "Call client regarding missing documents". */
    FOLLOW_UP,
    /** Reminder to collect documents from a client for an engagement. */
    DOCUMENT_COLLECTION,
    /** Generic reminder not tied to a specific system event. */
    GENERAL
}
