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
    GENERAL,
    /** Phase 32.6: Pending client approval reminder. */
    CLIENT_APPROVAL,
    /** Phase 32.6: Outstanding client document request reminder. */
    DOCUMENT_REQUEST,
    /** Phase 32.6: Unanswered client or practitioner message reminder. */
    UNANSWERED_COMMUNICATION,
    /** Phase 32.6: Upcoming compliance statutory deadline reminder. */
    COMPLIANCE_DEADLINE,
    /** Phase 32.6: Practitioner follow-up on overdue or blocked action. */
    PRACTITIONER_FOLLOW_UP
}
