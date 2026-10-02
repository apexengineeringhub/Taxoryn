package com.taxoryn.module.reminder.dto;

import com.taxoryn.module.reminder.entity.AutomationEventType;
import com.taxoryn.module.reminder.entity.AutomationTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating or updating an automation rule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update an automation rule")
public class SaveAutomationRuleRequest {

    @NotBlank(message = "Rule name is required")
    @Size(max = 255)
    @Schema(description = "Human-readable rule name", example = "Task Due Soon Reminder")
    private String name;

    @Size(max = 1000)
    @Schema(description = "Optional description of what this rule does")
    private String description;

    @NotNull(message = "Event type is required")
    @Schema(description = "Business event that triggers this rule", example = "TASK_DUE")
    private AutomationEventType eventType;

    @NotNull(message = "Days offset is required")
    @Schema(description = "Days offset relative to event: negative = before, 0 = on event, positive = after", example = "-2")
    private Integer daysOffset;

    @Schema(description = "Who should receive the action", example = "TASK_ASSIGNEE")
    @Builder.Default
    private AutomationTargetType targetType = AutomationTargetType.TASK_ASSIGNEE;

    @Schema(description = "Whether this rule is active", example = "true")
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;
}
