package com.taxoryn.module.notification.entity;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "client_id")
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    @Builder.Default
    private NotificationType notificationType = NotificationType.GENERAL;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "channels", nullable = false, length = 100)
    @Builder.Default
    private String channels = "IN_APP";

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "action_url")
    private String actionUrl;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 50)
    @Builder.Default
    private DeliveryStatus emailStatus = DeliveryStatus.NOT_REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "sms_status", nullable = false, length = 50)
    @Builder.Default
    private DeliveryStatus smsStatus = DeliveryStatus.NOT_REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "whatsapp_status", nullable = false, length = 50)
    @Builder.Default
    private DeliveryStatus whatsappStatus = DeliveryStatus.NOT_REQUESTED;

    public enum NotificationType {
        TASK_ASSIGNED,
        TASK_DUE,
        TASK_OVERDUE,
        DOCUMENT_REQUIRED,
        GST_DUE,
        ITR_DUE,
        PAYMENT_DUE,
        GENERAL
    }

    public enum NotificationChannel {
        IN_APP,
        EMAIL,
        SMS,
        WHATSAPP
    }

    public enum DeliveryStatus {
        NOT_REQUESTED,
        PENDING,
        SENT,
        FAILED
    }
}
