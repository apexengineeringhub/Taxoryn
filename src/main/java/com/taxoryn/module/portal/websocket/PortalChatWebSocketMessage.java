package com.taxoryn.module.portal.websocket;

import com.taxoryn.module.portal.dto.ClientPortalMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalChatWebSocketMessage {

    public enum Type {
        MESSAGE_RECEIVED,
        MESSAGES_READ,
        TYPING_START,
        TYPING_STOP,
        PING,
        PONG
    }

    private Type type;
    private UUID organizationId;
    private UUID clientId;
    private ClientPortalMessageDto message;
    private String senderName;
    private String timestamp;
}
