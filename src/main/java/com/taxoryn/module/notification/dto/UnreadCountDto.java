package com.taxoryn.module.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unread In-App Notification Count")
public class UnreadCountDto {

    @Schema(description = "Number of unread notifications for the current recipient", example = "7")
    private long unreadCount;
}
