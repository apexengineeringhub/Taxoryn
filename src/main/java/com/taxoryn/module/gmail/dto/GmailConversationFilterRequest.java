package com.taxoryn.module.gmail.dto;

import com.taxoryn.core.dto.PageRequestDto;
import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Criteria for searching and filtering tracked Gmail conversations")
public class GmailConversationFilterRequest extends PageRequestDto {

    @Schema(description = "Search keyword in subject, snippet, or sender name/email")
    private String search;

    @Schema(description = "Filter by workflow status")
    private GmailConversationStatus status;

    @Schema(description = "Filter by priority level")
    private GmailConversationPriority priority;

    @Schema(description = "Filter by associated client ID")
    private UUID clientId;

    @Schema(description = "Filter by practice location ID")
    private UUID locationId;

    @Schema(description = "Filter by assigned practitioner/user ID")
    private UUID assignedUserId;

    @Schema(description = "Filter only unassigned conversations")
    private Boolean unassignedOnly;

    @Schema(description = "Filter only conversations with unlinked client")
    private Boolean unlinkedClientOnly;

    @Schema(description = "Filter only unread conversations")
    private Boolean unreadOnly;

    @Schema(description = "Filter by connected mailbox / Gmail account ID")
    private UUID gmailAccountId;

    @Schema(description = "Filter conversations updated on or after this timestamp")
    private Instant fromDate;

    @Schema(description = "Filter conversations updated on or before this timestamp")
    private Instant toDate;
}
