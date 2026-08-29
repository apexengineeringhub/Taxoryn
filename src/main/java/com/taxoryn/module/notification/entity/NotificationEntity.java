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

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    @Builder.Default
    private Severity severity = Severity.INFO;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    @Builder.Default
    private Category category = Category.SYSTEM;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "expires_at")
    private Instant expiresAt;

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

    public enum Severity {
        INFO,
        SUCCESS,
        WARNING,
        ACTION_REQUIRED
    }

    public enum Category {
        CLIENT,
        DOCUMENT,
        TASK,
        COMPLIANCE,
        ACCOUNT,
        BILLING,
        SYSTEM
    }

    public enum NotificationType {
        CLIENT_REGISTERED,
        DOCUMENT_REQUEST_CREATED,
        DOCUMENT_UPLOADED,
        DOCUMENT_REJECTED,
        DOCUMENT_ACCEPTED,
        DOCUMENT_REQUEST_COMPLETED,
        DOCUMENT_REQUIRED,
        TASK_ASSIGNED,
        TASK_DUE,
        TASK_OVERDUE,
        TASK_BLOCKED,
        TASK_COMPLETED,
        COMPLIANCE_DUE,
        COMPLIANCE_OVERDUE,
        COMPLIANCE_COMPLETED,
        GST_DUE,
        GST_FILING_CREATED,
        GST_FILING_READY_FOR_REVIEW,
        GST_FILING_COMPLETED,
        ITR_DUE,
        PAYMENT_DUE,
        PAYMENT_RECEIVED,
        INVOICE_ISSUED,
        PASSWORD_CHANGED,
        PASSWORD_RESET_COMPLETED,
        SYSTEM_NOTIFICATION,
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
