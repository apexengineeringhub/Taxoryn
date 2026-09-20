package com.taxoryn.module.portal.entity;

import com.taxoryn.core.domain.TenantAuditableEntity;
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
@Table(name = "client_portal_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientPortalMessageEntity extends TenantAuditableEntity {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private PortalMessageSenderType senderType; // CLIENT, PRACTICE, SYSTEM

    @Column(name = "sender_user_id")
    private UUID senderUserId;

    @Column(name = "sender_name", nullable = false, length = 150)
    private String senderName;

    @Column(name = "sender_email")
    private String senderEmail;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "attachments_json", columnDefinition = "TEXT")
    private String attachmentsJson;

    @Column(name = "is_read_by_client", nullable = false)
    @Builder.Default
    private boolean readByClient = false;

    @Column(name = "is_read_by_practice", nullable = false)
    @Builder.Default
    private boolean readByPractice = false;

    @Column(name = "read_at")
    private Instant readAt;
}
