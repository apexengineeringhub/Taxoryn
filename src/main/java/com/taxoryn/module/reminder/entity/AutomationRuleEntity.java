package com.taxoryn.module.reminder.entity;

import com.taxoryn.core.domain.AuditableEntity;
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

import java.util.UUID;

/**
 * A configurable rule that defines when and how reminders are created.
 *
 * <p>Rules are evaluated when a matching {@code AutomationEventType} fires in the system.
 * They can be:
 * <ul>
 *   <li><b>System defaults</b> — {@code organizationId = null}, apply to all organizations.</li>
 *   <li><b>Org-specific</b> — {@code organizationId} is set, override system defaults for that org.</li>
 * </ul>
 *
 * <p>Extends {@code AuditableEntity} (not {@code TenantAuditableEntity}) because system-default
 * rules have no organization owner. The {@code TenantAuditableEntity}'s {@code @PrePersist}
 * would reject a null organizationId; we handle org scoping manually in the service layer.
 *
 * <p>Example rule: {@code TASK_DUE} event → offset of {@code -2} days → action {@code CREATE_REMINDER}
 * → target {@code TASK_ASSIGNEE}.
 */
@Entity
@Table(name = "automation_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRuleEntity extends AuditableEntity {

    /**
     * Organization that owns this rule.
     * Null means this is a system-wide default rule that applies to all organizations.
     */
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The business event this rule reacts to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private AutomationEventType eventType;

    /**
     * Offset in days relative to the event date.
     * <ul>
     *   <li>{@code -2} = 2 days <em>before</em> (e.g. task due date)</li>
     *   <li>{@code  0} = same day as the event</li>
     *   <li>{@code  1} = 1 day <em>after</em> the event</li>
     * </ul>
     */
    @Column(name = "days_offset", nullable = false)
    @Builder.Default
    private Integer daysOffset = -1;

    /**
     * What happens when the rule fires.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 80)
    @Builder.Default
    private AutomationActionType actionType = AutomationActionType.CREATE_REMINDER;

    /**
     * Who should receive the reminder/notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 80)
    @Builder.Default
    private AutomationTargetType targetType = AutomationTargetType.TASK_ASSIGNEE;

    /**
     * Whether this rule is currently active.
     * Disabled rules are skipped by the automation processor.
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;
}
