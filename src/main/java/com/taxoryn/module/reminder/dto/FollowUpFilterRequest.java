package com.taxoryn.module.reminder.dto;

import com.taxoryn.module.reminder.entity.ReminderPriority;
import com.taxoryn.module.reminder.entity.ReminderStatus;
import com.taxoryn.module.reminder.entity.ReminderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter parameters for follow-up reminders")
public class FollowUpFilterRequest {

    private UUID clientId;
    private ReminderType category;
    private ReminderStatus status;
    private ReminderPriority priority;
    private Boolean overdueOnly;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
