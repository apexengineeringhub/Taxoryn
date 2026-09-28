package com.taxoryn.module.gmail.entity;

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
@Table(name = "gmail_conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmailConversationEntity extends TenantAuditableEntity {

    @Column(name = "gmail_account_id", nullable = false)
    private UUID gmailAccountId;

    @Column(name = "thread_id", nullable = false, length = 100)
    private String threadId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private GmailConversationStatus status = GmailConversationStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private GmailConversationPriority priority = GmailConversationPriority.NORMAL;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "snippet", length = 1000)
    private String snippet;

    @Column(name = "sender_email", length = 255)
    private String senderEmail;

    @Column(name = "sender_name", length = 255)
    private String senderName;

    @Column(name = "recipient_emails", columnDefinition = "TEXT")
    private String recipientEmails;

    @Column(name = "message_count", nullable = false)
    @Builder.Default
    private Integer messageCount = 1;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "is_unread", nullable = false)
    @Builder.Default
    private Boolean isUnread = true;

    @Column(name = "is_starred", nullable = false)
    @Builder.Default
    private Boolean isStarred = false;

    @Column(name = "gmail_labels", length = 500)
    private String gmailLabels;

    @Column(name = "web_link", length = 1000)
    private String webLink;
}
