package com.taxoryn.module.notification.event;

import com.taxoryn.module.notification.entity.NotificationEntity.Category;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationChannel;
import com.taxoryn.module.notification.entity.NotificationEntity.NotificationType;
import com.taxoryn.module.notification.entity.NotificationEntity.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Standard decoupled application event for broadcasting in-app notifications
 * across any module within Taxoryn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxorynNotificationEvent {

    private UUID organizationId;
    private UUID userId;
    private UUID clientId;
    private NotificationType notificationType;
    private Severity severity;
    private Category category;
    private String entityType;
    private String entityId;
    private String title;
    private String message;
    private Set<NotificationChannel> channels;
    private String actionUrl;
    private String metadata;
    private Instant expiresAt;
}