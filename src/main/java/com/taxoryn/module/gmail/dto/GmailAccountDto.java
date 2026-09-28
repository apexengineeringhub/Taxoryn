package com.taxoryn.module.gmail.dto;

import com.taxoryn.module.gmail.entity.GmailAccountStatus;
import com.taxoryn.module.gmail.entity.GmailAccountType;
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
@Schema(description = "Connected Gmail Account Profile and Status")
public class GmailAccountDto {

    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String userFullName;
    private String emailAddress;
    private GmailAccountType accountType;
    private GmailAccountStatus status;
    private Instant lastSyncedAt;
    private String lastHistoryId;
    private String syncErrorMessage;
    private long totalConversations;
    private long unreadConversations;
    private long openConversations;
    private Instant createdAt;
    private Instant updatedAt;
}
