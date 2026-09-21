package com.taxoryn.module.employee.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeChatWebSocketMessage {

    public enum Type {
        PING,
        PONG,
        NEW_MESSAGE,
        TYPING_START,
        TYPING_STOP,
        MESSAGES_READ
    }

    private Type type;
    private UUID organizationId;
    private UUID channelId;
    private UUID senderEmployeeId;
    private String senderName;
    private UUID recipientEmployeeId;
    private EmployeeChatMessageDto message;
    private String timestamp;
}
