package com.taxoryn.module.notification.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.notification.entity.NotificationEntity.Category;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.entity.NotificationEntity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Notification History Search, Filter, and Pagination Parameters")
public class NotificationFilterRequest extends PageRequestDto {

    @Schema(description = "Filter by read/unread status. Omit to return all.", example = "false")
    private Boolean isRead;

    @Schema(description = "Filter by notification category", example = "DOCUMENT")
    private Category category;

    @Schema(description = "Filter by notification severity", example = "ACTION_REQUIRED")
    private Severity severity;

    @Schema(description = "Filter by notification type", example = "TASK_DUE")
    private NotificationType notificationType;
}
