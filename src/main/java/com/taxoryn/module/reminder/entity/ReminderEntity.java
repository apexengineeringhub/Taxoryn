package com.taxoryn.module.reminder.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single actionable reminder for a practitioner or team member.
 *
 * <p>Reminders are either:
 * <ul>
 *   <li><b>Manual</b> — created directly by a user via the API (automationRuleId = null)</li>
 *   <li><b>Automation-generated</b> — created by {@code ReminderScheduler} or {@code ReminderEventListener}
 *       based on an {@code AutomationRule} reacting to a business event.</li>
 * </ul>
 *
 * <p>Idempotency is enforced via the {@code idempotency_key} unique constraint.
 * For automation-generated reminders the key follows the pattern:
 * {@code <ruleId>::<referenceType>::<referenceId>::<yyyy-MM-dd>}
 *
 * <p>The {@code status} field is the source of truth for the reminder lifecycle.
 * OVERDUE is a derived condition: {@code status == PENDING && scheduledAt < Instant.now()}.
 */
@Entity
@Table(name = "reminders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderEntity extends TenantAuditableEntity {

    // -------------------------------------------------------------------------
    // Content
    // -------------------------------------------------------------------------

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 50)
    @Builder.Default
    private ReminderType reminderType = ReminderType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ReminderStatus status = ReminderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    @Builder.Default
    private ReminderPriority priority = ReminderPriority.MEDIUM;

    // -------------------------------------------------------------------------
    // Relationships — all optional, any can be set simultaneously
    // -------------------------------------------------------------------------

    /** The firm user who should be reminded. Null only for client-directed reminders (rare). */
    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "engagement_id")
    private UUID engagementId;

    @Column(name = "work_instance_id")
    private UUID workInstanceId;

    @Column(name = "task_id")
    private UUID taskId;

    // -------------------------------------------------------------------------
    // Automation linkage
    // -------------------------------------------------------------------------

    /** Which automation rule generated this reminder. Null for manual reminders. */
    @Column(name = "automation_rule_id")
    private UUID automationRuleId;

    /** Coarse entity type for idempotency/routing. E.g. "TASK", "WORK_INSTANCE". */
    @Column(name = "reference_type", length = 64)
    private String referenceType;

    /** UUID string of the referenced entity for idempotency/routing. */
    @Column(name = "reference_id", length = 64)
    private String referenceId;

    /**
     * Deterministic deduplication key for automation-generated reminders.
     * Pattern: {@code <ruleId>::<referenceType>::<referenceId>::<yyyy-MM-dd>}
     * Null for manually created reminders (no uniqueness restriction).
     */
    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;

    // -------------------------------------------------------------------------
    // Scheduling
    // -------------------------------------------------------------------------

    /** The moment this reminder should be triggered by the scheduler. */
    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 30)
    @Builder.Default
    private ReminderRecurrenceType recurrenceType = ReminderRecurrenceType.NONE;

    // -------------------------------------------------------------------------
    // Lifecycle timestamps
    // -------------------------------------------------------------------------

    /** Set when the scheduler fires the reminder and creates a Notification. */
    @Column(name = "triggered_at")
    private Instant triggeredAt;

    /** Set when the user marks the reminder as completed. */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** Set when the reminder is cancelled (e.g. the underlying task was cancelled). */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // -------------------------------------------------------------------------
    // Derived helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if this reminder is overdue — meaning it was not yet triggered
     * and its scheduled time has already passed.
     */
    public boolean isOverdue() {
        return status == ReminderStatus.PENDING && scheduledAt != null && Instant.now().isAfter(scheduledAt);
    }
}
