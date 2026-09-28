package com.taxoryn.module.gmail.dto;

import com.taxoryn.module.gmail.entity.GmailConversationPriority;
import com.taxoryn.module.gmail.entity.GmailConversationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to update conversation status, assignment, or priority")
public class GmailConversationUpdateDto {

    @Schema(description = "New workflow status")
    private GmailConversationStatus status;

    @Schema(description = "New priority level")
    private GmailConversationPriority priority;

    @Schema(description = "Assigned practitioner/user ID (pass null or zero UUID to unassign)")
    private UUID assignedUserId;

    @Schema(description = "Associated practice location ID")
    private UUID locationId;

    @Schema(description = "Mark as read/unread flag")
    private Boolean isUnread;

    @Schema(description = "Mark as starred flag")
    private Boolean isStarred;
}
