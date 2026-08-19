package com.taxoryn.module.portal.dto;

import com.taxoryn.module.portal.entity.ClientNotificationEntity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client Portal Notification Details")
public class ClientNotificationDto {

    private UUID id;
    private UUID clientId;
    private String title;
    private String message;
    private NotificationType notificationType;
    private boolean read;
    private String actionUrl;
    private Instant createdAt;
}
