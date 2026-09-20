package com.taxoryn.module.portal.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortalChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    // Room map: ClientId -> Set of active WebSocket sessions
    private final Map<UUID, Set<WebSocketSession>> clientRooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null || !StringUtils.hasText(uri.getQuery())) {
            log.warn("Rejected WS connection without query parameters: id={}", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("Missing query parameters"));
            return;
        }

        Map<String, String> queryParams = parseQueryParams(uri.getQuery());
        String token = queryParams.get("token");
        String clientIdParam = queryParams.get("clientId");

        if (!StringUtils.hasText(token)) {
            log.warn("Rejected WS connection without authentication token: id={}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing auth token"));
            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Rejected WS connection with invalid/expired token: id={}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or expired token"));
                return;
            }

            UUID tokenUserId = jwtTokenProvider.getUserIdFromToken(token);
            UUID tokenOrgId = jwtTokenProvider.getOrganizationIdFromToken(token);
            UUID tokenClientId = jwtTokenProvider.getClientIdFromToken(token);

            UUID targetClientId;
            if (tokenClientId != null) {
                // Client user: strictly restricted to their own client room
                targetClientId = tokenClientId;
            } else if (StringUtils.hasText(clientIdParam)) {
                // Practice staff user: managing specific client consultation chat
                targetClientId = UUID.fromString(clientIdParam);
            } else {
                log.warn("WS connection missing target clientId: user={}", tokenUserId);
                session.close(CloseStatus.BAD_DATA.withReason("Missing clientId parameter"));
                return;
            }

            session.getAttributes().put("userId", tokenUserId);
            session.getAttributes().put("organizationId", tokenOrgId);
            session.getAttributes().put("clientId", targetClientId);

            clientRooms.computeIfAbsent(targetClientId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(session);

            log.info("WebSocket connected for client room: clientId={}, userId={}, session={}",
                    targetClientId, tokenUserId, session.getId());

            // Acknowledge connection
            PortalChatWebSocketMessage ack = PortalChatWebSocketMessage.builder()
                    .type(PortalChatWebSocketMessage.Type.PONG)
                    .clientId(targetClientId)
                    .organizationId(tokenOrgId)
                    .timestamp(java.time.Instant.now().toString())
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));

        } catch (Exception e) {
            log.warn("Error establishing WS connection: session={}, error={}", session.getId(), e.getMessage());
            session.close(CloseStatus.SERVER_ERROR.withReason("Handshake failed"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (!StringUtils.hasText(payload)) return;

        try {
            PortalChatWebSocketMessage wsMsg = objectMapper.readValue(payload, PortalChatWebSocketMessage.class);
            if (wsMsg.getType() == PortalChatWebSocketMessage.Type.PING) {
                PortalChatWebSocketMessage pong = PortalChatWebSocketMessage.builder()
                        .type(PortalChatWebSocketMessage.Type.PONG)
                        .clientId((UUID) session.getAttributes().get("clientId"))
                        .organizationId((UUID) session.getAttributes().get("organizationId"))
                        .timestamp(java.time.Instant.now().toString())
                        .build();
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
            } else if (wsMsg.getType() == PortalChatWebSocketMessage.Type.TYPING_START
                    || wsMsg.getType() == PortalChatWebSocketMessage.Type.TYPING_STOP) {
                UUID clientId = (UUID) session.getAttributes().get("clientId");
                if (clientId != null) {
                    broadcastToClientRoomExceptSender(clientId, wsMsg, session);
                }
            }
        } catch (Exception e) {
            log.debug("Error processing incoming WS message from session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID clientId = (UUID) session.getAttributes().get("clientId");
        if (clientId != null) {
            Set<WebSocketSession> sessions = clientRooms.get(clientId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    clientRooms.remove(clientId);
                }
            }
        }
        log.info("WebSocket session closed: id={}, status={}", session.getId(), status);
    }

    public void broadcastToClientRoom(UUID clientId, PortalChatWebSocketMessage message) {
        if (clientId == null || message == null) return;
        Set<WebSocketSession> sessions = clientRooms.get(clientId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonPayload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send WS message to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize and broadcast WS message for client {}: {}", clientId, e.getMessage());
        }
    }

    public void broadcastToClientRoomExceptSender(UUID clientId, PortalChatWebSocketMessage message, WebSocketSession senderSession) {
        if (clientId == null || message == null) return;
        Set<WebSocketSession> sessions = clientRooms.get(clientId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonPayload);

            for (WebSocketSession session : sessions) {
                if (session.isOpen() && !Objects.equals(session.getId(), senderSession.getId())) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send WS message to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize and broadcast WS message for client {}: {}", clientId, e.getMessage());
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new ConcurrentHashMap<>();
        if (!StringUtils.hasText(query)) return params;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                params.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else if (idx > 0) {
                params.put(pair.substring(0, idx), "");
            }
        }
        return params;
    }
}
