package com.taxoryn.module.reminder.entity;

/**
 * What the automation rule does when its event fires.
 *
 * EMAIL, WHATSAPP, SMS are extension points — not implemented in P0.5.
 */
public enum AutomationActionType {
    /** Create a ReminderEntity (processed by ReminderScheduler into a Notification). */
    CREATE_REMINDER,
    /** Directly create an in-app NotificationEntity without a Reminder record. */
    IN_APP_NOTIFICATION
}
