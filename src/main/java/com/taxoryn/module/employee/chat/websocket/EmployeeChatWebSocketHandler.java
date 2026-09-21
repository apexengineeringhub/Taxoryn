package com.taxoryn.module.employee.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxoryn.core.security.JwtTokenProvider;
import com.taxoryn.module.employee.chat.dto.EmployeeChatWebSocketMessage;
import com.taxoryn.module.employee.entity.EmployeeEntity;
import com.taxoryn.module.employee.repository.EmployeeRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;

    // Map: OrganizationId -> (EmployeeId -> Set of WebSocketSessions)
    private final Map<UUID, Map<UUID, Set<WebSocketSession>>> orgEmployeeSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null || !StringUtils.hasText(uri.getQuery())) {
            log.warn("Rejected employee WS connection without query params: id={}", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("Missing query parameters"));
            return;
        }

        Map<String, String> queryParams = parseQueryParams(uri.getQuery());
        String token = queryParams.get("token");

        if (!StringUtils.hasText(token)) {
            log.warn("Rejected employee WS connection without auth token: id={}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing auth token"));
            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Rejected employee WS connection with invalid token: id={}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or expired token"));
                return;
            }

            UUID tokenUserId = jwtTokenProvider.getUserIdFromToken(token);
            UUID tokenOrgId = jwtTokenProvider.getOrganizationIdFromToken(token);

            if (tokenOrgId == null || tokenUserId == null) {
                log.warn("Rejected employee WS missing org or user claim: user={}, org={}", tokenUserId, tokenOrgId);
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Incomplete token context"));
                return;
            }

            Optional<EmployeeEntity> employeeOpt = employeeRepository.findByOrganizationIdAndUserId(tokenOrgId, tokenUserId);
            UUID employeeId = employeeOpt.map(EmployeeEntity::getId).orElse(null);

            session.getAttributes().put("userId", tokenUserId);
            session.getAttributes().put("organizationId", tokenOrgId);
            if (employeeId != null) {
                session.getAttributes().put("employeeId", employeeId);
            }

            // Register session in org employee map
            UUID trackingKey = employeeId != null ? employeeId : tokenUserId;
            orgEmployeeSessions
                    .computeIfAbsent(tokenOrgId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(trackingKey, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(session);

            log.info("Employee WebSocket connected: orgId={}, employeeId={}, userId={}, session={}",
                    tokenOrgId, employeeId, tokenUserId, session.getId());

            // Acknowledge connection with PONG
            EmployeeChatWebSocketMessage ack = EmployeeChatWebSocketMessage.builder()
                    .type(EmployeeChatWebSocketMessage.Type.PONG)
                    .organizationId(tokenOrgId)
                    .senderEmployeeId(employeeId)
                    .timestamp(java.time.Instant.now().toString())
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));

        } catch (Exception e) {
            log.warn("Error establishing employee WS connection: session={}, error={}", session.getId(), e.getMessage());
            session.close(CloseStatus.SERVER_ERROR.withReason("Handshake failed"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (!StringUtils.hasText(payload)) return;

        try {
            EmployeeChatWebSocketMessage wsMsg = objectMapper.readValue(payload, EmployeeChatWebSocketMessage.class);
            UUID orgId = (UUID) session.getAttributes().get("organizationId");
            UUID employeeId = (UUID) session.getAttributes().get("employeeId");

            if (wsMsg.getType() == EmployeeChatWebSocketMessage.Type.PING) {
                EmployeeChatWebSocketMessage pong = EmployeeChatWebSocketMessage.builder()
                        .type(EmployeeChatWebSocketMessage.Type.PONG)
                        .organizationId(orgId)
                        .senderEmployeeId(employeeId)
                        .timestamp(java.time.Instant.now().toString())
                        .build();
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
            } else if (wsMsg.getType() == EmployeeChatWebSocketMessage.Type.TYPING_START
                    || wsMsg.getType() == EmployeeChatWebSocketMessage.Type.TYPING_STOP) {
                // Forward typing indicator to recipient
                if (wsMsg.getRecipientEmployeeId() != null && orgId != null) {
                    wsMsg.setOrganizationId(orgId);
                    wsMsg.setSenderEmployeeId(employeeId);
                    sendToEmployee(orgId, wsMsg.getRecipientEmployeeId(), wsMsg);
                }
            }
        } catch (Exception e) {
            log.debug("Error processing incoming employee WS message: session={}, error={}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID orgId = (UUID) session.getAttributes().get("organizationId");
        UUID employeeId = (UUID) session.getAttributes().get("employeeId");
        UUID userId = (UUID) session.getAttributes().get("userId");
        UUID trackingKey = employeeId != null ? employeeId : userId;

        if (orgId != null && trackingKey != null) {
            Map<UUID, Set<WebSocketSession>> empMap = orgEmployeeSessions.get(orgId);
            if (empMap != null) {
                Set<WebSocketSession> sessions = empMap.get(trackingKey);
                if (sessions != null) {
                    sessions.remove(session);
                    if (sessions.isEmpty()) {
                        empMap.remove(trackingKey);
                    }
                }
                if (empMap.isEmpty()) {
                    orgEmployeeSessions.remove(orgId);
                }
            }
        }
        log.info("Employee WebSocket session closed: id={}, status={}", session.getId(), status);
    }

    public void sendToEmployee(UUID organizationId, UUID employeeId, EmployeeChatWebSocketMessage message) {
        if (organizationId == null || employeeId == null || message == null) return;
        Map<UUID, Set<WebSocketSession>> empMap = orgEmployeeSessions.get(organizationId);
        if (empMap == null) return;

        Set<WebSocketSession> sessions = empMap.get(employeeId);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonPayload);

            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    try {
                        s.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed to send employee WS message to session {}: {}", s.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize employee WS message: {}", e.getMessage());
        }
    }

    public void broadcastToChannelMembers(UUID organizationId, Set<UUID> memberEmployeeIds, EmployeeChatWebSocketMessage message) {
        if (organizationId == null || message == null || memberEmployeeIds == null || memberEmployeeIds.isEmpty()) return;

        for (UUID empId : memberEmployeeIds) {
            sendToEmployee(organizationId, empId, message);
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
