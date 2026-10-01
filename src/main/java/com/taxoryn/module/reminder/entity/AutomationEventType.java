package com.taxoryn.module.reminder.entity;

/**
 * Business events that AutomationRules can react to.
 *
 * Restricted to events cleanly supported by the current P0.3–P0.4 architecture.
 * Do NOT add speculative events that have no implementation.
 */
public enum AutomationEventType {
    /** A new Task has been created. */
    TASK_CREATED,
    /** A Task has been assigned to a user. */
    TASK_ASSIGNED,
    /** A Task is approaching or on its due date. */
    TASK_DUE,
    /** A Task has passed its due date without being completed. */
    TASK_OVERDUE,
    /** A Task has been marked as completed. */
    TASK_COMPLETED,
    /** A Document has been uploaded (any context). */
    DOCUMENT_UPLOADED,
    /** A new Work Instance has been generated from a Work Template. */
    WORK_INSTANCE_CREATED,
    /** A Work Instance is approaching or on its due date. */
    WORK_INSTANCE_DUE
}
