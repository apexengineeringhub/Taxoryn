package com.taxoryn.module.portal.websocket;

import com.taxoryn.module.portal.dto.ClientPortalMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalChatEventPublisher {

    private final PortalChatWebSocketHandler webSocketHandler;

    public void publishNewMessage(UUID organizationId, UUID clientId, ClientPortalMessageDto message) {
        try {
            PortalChatWebSocketMessage wsMessage = PortalChatWebSocketMessage.builder()
                    .type(PortalChatWebSocketMessage.Type.MESSAGE_RECEIVED)
                    .organizationId(organizationId)
                    .clientId(clientId)
                    .message(message)
                    .senderName(message.getSenderName())
                    .timestamp(Instant.now().toString())
                    .build();

            webSocketHandler.broadcastToClientRoom(clientId, wsMessage);
        } catch (Exception e) {
            log.warn("Failed to publish real-time WS message event for client {}: {}", clientId, e.getMessage());
        }
    }

    public void publishMessagesRead(UUID organizationId, UUID clientId, String readerType) {
        try {
            PortalChatWebSocketMessage wsMessage = PortalChatWebSocketMessage.builder()
                    .type(PortalChatWebSocketMessage.Type.MESSAGES_READ)
                    .organizationId(organizationId)
                    .clientId(clientId)
                    .senderName(readerType)
                    .timestamp(Instant.now().toString())
                    .build();

            webSocketHandler.broadcastToClientRoom(clientId, wsMessage);
        } catch (Exception e) {
            log.warn("Failed to publish real-time WS read event for client {}: {}", clientId, e.getMessage());
        }
    }
}
