package com.taxoryn.module.employee.chat.dto;

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
public class EmployeeChatMessageDto {
    private UUID id;
    private UUID organizationId;
    private UUID channelId;
    private UUID senderEmployeeId;
    private String senderName;
    private String senderEmail;
    private String senderAvatarUrl;
    private String senderDesignation;
    private String senderDepartment;
    private UUID recipientEmployeeId;
    private String recipientName;
    private String messageBody;
    private String attachmentsJson;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
    private Boolean isOwnMessage;
}
