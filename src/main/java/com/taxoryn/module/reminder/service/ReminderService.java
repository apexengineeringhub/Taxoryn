package com.taxoryn.module.reminder.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.reminder.dto.CreateReminderRequest;
import com.taxoryn.module.reminder.dto.ReminderDto;
import com.taxoryn.module.reminder.dto.ReminderFilterRequest;
import com.taxoryn.module.reminder.dto.UpdateReminderRequest;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import com.taxoryn.module.reminder.event.TaxorynBusinessEvent;

import java.util.List;
import java.util.UUID;

/**
 * Core reminder business operations.
 *
 * <p>Two consumer paths:
 * <ol>
 *   <li><b>Manual</b> — practitioners call {@link #createReminder} directly.</li>
 *   <li><b>Automated</b> — {@code ReminderEventListener} calls
 *       {@link #createAutomatedReminder} after evaluating automation rules.</li>
 * </ol>
 *
 * <p>Scheduler path: {@code ReminderScheduler} calls {@link #processAllDueReminders}
 * once daily to fire PENDING reminders and create notifications.
 */
public interface ReminderService {

    // -------------------------------------------------------------------------
    // CRUD (manual reminders from REST API)
    // -------------------------------------------------------------------------

    /** Create a manual reminder for the current authenticated user. */
    ReminderDto createReminder(CreateReminderRequest request);

    /** Update a reminder. Only PENDING reminders can be updated. */
    ReminderDto updateReminder(UUID reminderId, UpdateReminderRequest request);

    /** Get a single reminder by ID (org-scoped). */
    ReminderDto getReminderById(UUID reminderId);

    /**
     * Get paginated list of reminders for the current user (My Reminders).
     */
    PagedResponse<ReminderDto> getMyReminders(ReminderFilterRequest filter);

    /**
     * Get paginated list of all reminders for the current org (Team view — admins only).
     */
    PagedResponse<ReminderDto> getTeamReminders(ReminderFilterRequest filter);

    /**
     * Get reminders linked to a specific task.
     */
    List<ReminderDto> getRemindersForTask(UUID taskId);

    // -------------------------------------------------------------------------
    // Lifecycle transitions
    // -------------------------------------------------------------------------

    /** Mark a reminder as completed. Can be called from the UI. */
    ReminderDto completeReminder(UUID reminderId);

    /** Cancel a reminder. Can be called from the UI or task lifecycle hooks. */
    ReminderDto cancelReminder(UUID reminderId);

    /**
     * Cancel all PENDING reminders linked to a specific task.
     * Called when the task itself is cancelled or completed.
     *
     * @return number of reminders cancelled
     */
    int cancelRemindersForTask(UUID organizationId, UUID taskId);

    // -------------------------------------------------------------------------
    // Automation integration
    // -------------------------------------------------------------------------

    /**
     * Creates an automation-generated reminder with idempotency enforcement.
     * The idempotency key is computed and checked before persisting.
     *
     * @param event    the business event that triggered the rule
     * @param rule     the automation rule that matched
     * @return the created ReminderDto, or null if a duplicate was detected
     */
    ReminderDto createAutomatedReminder(TaxorynBusinessEvent event, AutomationRuleEntity rule);

    // -------------------------------------------------------------------------
    // Scheduler integration
    // -------------------------------------------------------------------------

    /**
     * Called daily by {@code ReminderScheduler} to process all PENDING reminders
     * whose scheduledAt ≤ now, create notifications, and mark them as TRIGGERED.
     *
     * @param organizationId the org being processed (null = all orgs, for global scan)
     * @return number of reminders triggered
     */
    int processAllDueReminders(UUID organizationId);

    /**
     * Count of overdue PENDING reminders for the current user.
     * Used for dashboard badge.
     */
    long countOverdueForCurrentUser();
}
