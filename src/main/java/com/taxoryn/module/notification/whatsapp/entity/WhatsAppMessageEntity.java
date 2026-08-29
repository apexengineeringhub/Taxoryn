package com.taxoryn.module.notification.whatsapp.entity;

import com.taxoryn.core.domain.AuditableEntity;
import com.taxoryn.module.organization.entity.OrganizationEntity;
import com.taxoryn.module.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppMessageEntity extends AuditableEntity {

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "recipient_phone", nullable = false, length = 50)
    private String recipientPhone;

    @Column(name = "template_type", nullable = false, length = 100)
    private String templateType;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private String provider = "LOG";

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private WhatsAppMessageStatus status = WhatsAppMessageStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;
}
