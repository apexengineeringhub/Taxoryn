package com.taxoryn.module.reminder.entity;

/**
 * Priority of a reminder — mirrors TaskPriority for consistency.
 * Priority is independent from ReminderStatus.
 */
public enum ReminderPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
