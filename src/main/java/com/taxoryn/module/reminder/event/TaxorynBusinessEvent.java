package com.taxoryn.module.reminder.event;

import com.taxoryn.module.reminder.entity.AutomationEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Generic application event published when a business action occurs that
 * automation rules should evaluate.
 *
 * <p>This is a plain Spring application event (no Kafka, no external bus).
 * Published via {@code ApplicationEventPublisher} after a transactional operation.
 * Consumed by {@link ReminderEventListener} which is {@code @Async} +
 * {@code @TransactionalEventListener(AFTER_COMMIT)} to guarantee the entity
 * exists in the DB before automation rules run.
 *
 * <p><strong>Do NOT add domain logic here.</strong> This is a pure data carrier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxorynBusinessEvent {

    /** The organization this event belongs to. Never null. */
    private UUID organizationId;

    /** The type of event. Used to look up applicable AutomationRules. */
    private AutomationEventType eventType;

    // -------------------------------------------------------------------------
    // Entity references — only the relevant ones are populated per event type
    // -------------------------------------------------------------------------

    /** Task ID — populated for TASK_* event types. */
    private UUID taskId;

    /** User assigned to the task (may be an Employee UUID or User UUID). */
    private UUID assignedUserId;

    /** Due date of the task / work instance. Used to compute scheduled_at offsets. */
    private LocalDate dueDate;

    /** Client linked to the entity. */
    private UUID clientId;

    /** Engagement linked to the entity. */
    private UUID engagementId;

    /** Work instance ID — populated for WORK_INSTANCE_* event types. */
    private UUID workInstanceId;

    /** Short display title of the entity (e.g. task title) for reminder text generation. */
    private String entityTitle;
}
