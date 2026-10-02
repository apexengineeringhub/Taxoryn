package com.taxoryn.module.reminder.dto;

import com.taxoryn.module.reminder.entity.AutomationEventType;
import com.taxoryn.module.reminder.entity.AutomationRuleEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an AutomationRule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Automation rule configuration")
public class AutomationRuleDto {

    private UUID id;

    @Schema(description = "Organization that owns this rule. Null = system default.")
    private UUID organizationId;

    private String name;
    private String description;

    @Schema(description = "Event that triggers this rule", example = "TASK_DUE")
    private AutomationEventType eventType;

    @Schema(description = "Day offset relative to event: negative = before, 0 = same day, positive = after", example = "-2")
    private Integer daysOffset;

    @Schema(description = "Human-readable offset description", example = "2 days before due date")
    private String offsetDescription;

    @Schema(description = "Action taken when rule fires", example = "CREATE_REMINDER")
    private String actionType;

    @Schema(description = "Who is targeted by the action", example = "TASK_ASSIGNEE")
    private String targetType;

    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Factory method for clean mapping from entity.
     */
    public static AutomationRuleDto from(AutomationRuleEntity e) {
        return AutomationRuleDto.builder()
                .id(e.getId())
                .organizationId(e.getOrganizationId())
                .name(e.getName())
                .description(e.getDescription())
                .eventType(e.getEventType())
                .daysOffset(e.getDaysOffset())
                .offsetDescription(describeOffset(e.getDaysOffset()))
                .actionType(e.getActionType() != null ? e.getActionType().name() : null)
                .targetType(e.getTargetType() != null ? e.getTargetType().name() : null)
                .enabled(e.getEnabled())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private static String describeOffset(Integer offset) {
        if (offset == null) return null;
        if (offset == 0) return "On the due date";
        if (offset < 0)  return Math.abs(offset) + " day" + (Math.abs(offset) > 1 ? "s" : "") + " before due date";
        return offset + " day" + (offset > 1 ? "s" : "") + " after due date";
    }
}
