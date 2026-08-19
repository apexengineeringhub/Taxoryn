package com.taxoryn.module.notification.dto;

import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dispatch Notification Request Payload")
public class SendNotificationRequest {

    @Schema(description = "Recipient firm user ID (optional if client targeted)")
    private UUID userId;

    @Schema(description = "Recipient client ID (optional if user targeted)")
    private UUID clientId;

    @NotNull(message = "Notification type is required")
    @Schema(description = "Type of notification", example = "TASK_ASSIGNED")
    private NotificationType notificationType;

    @NotBlank(message = "Title is required")
    @Schema(description = "Notification title", example = "New Task Assigned: GSTR-3B Review")
    private String title;

    @NotBlank(message = "Message content is required")
    @Schema(description = "Notification body message", example = "You have been assigned to prepare GSTR-3B for client ABC Traders.")
    private String message;

    @Schema(description = "Delivery channels (IN_APP, EMAIL, SMS, WHATSAPP)", example = "[\"IN_APP\", \"EMAIL\"]")
    @Builder.Default
    private Set<NotificationChannel> channels = Set.of(NotificationChannel.IN_APP);

    @Schema(description = "Action URL or deeplink", example = "/tasks/84729103-abcd")
    private String actionUrl;

    @Schema(description = "Context metadata / JSON payload", example = "{\"taskId\":\"84729103-abcd\"}")
    private String metadata;
}
