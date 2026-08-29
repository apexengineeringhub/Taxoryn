package com.taxoryn.module.notification.dto;

import com.taxoryn.module.notification.entity.NotificationEntity.Category;
import com.taxoryn.module.notification.entity.NotificationEntity.DeliveryStatus;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.entity.NotificationEntity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification Response DTO")
public class NotificationDto {

    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private UUID clientId;
    private String recipientName;
    private NotificationType notificationType;
    private Severity severity;
    private Category category;
    private String entityType;
    private String entityId;
    private String title;
    private String message;
    private Set<String> channels;
    private boolean isRead;
    private Instant readAt;
    private Instant expiresAt;
    private String actionUrl;
    private String metadata;
    private DeliveryStatus emailStatus;
    private DeliveryStatus smsStatus;
    private DeliveryStatus whatsappStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
