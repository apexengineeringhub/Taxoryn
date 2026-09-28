package com.taxoryn.module.gmail.dto;

import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
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
@Schema(description = "Gmail Tracked Conversation Metadata & Practice Context")
public class GmailConversationDto {

    private UUID id;
    private UUID organizationId;
    private UUID gmailAccountId;
    private String threadId;

    // Client context
    private UUID clientId;
    private String clientDisplayName;
    private String clientPan;
    private String clientGstin;

    // Location & Assignee context
    private UUID locationId;
    private String locationName;
    private UUID assignedUserId;
    private String assignedUserName;

    // Workflow & Metadata
    private GmailConversationStatus status;
    private GmailConversationPriority priority;
    private String category;
    private String mailboxEmail;
    private String subject;
    private String snippet;
    private String senderEmail;
    private String senderName;
    private String recipientEmails;
    private Integer messageCount;

    // Timestamps & SLA
    private Instant lastMessageAt;
    private Instant firstResponseAt;
    private Instant resolvedAt;
    private Boolean isUnread;
    private Boolean isStarred;
    private String gmailLabels;
    private String webLink;

    private Instant createdAt;
    private Instant updatedAt;
}
