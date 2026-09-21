package com.taxoryn.module.portal.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PortalChatWebSocketHandler portalChatWebSocketHandler;
    private final com.taxoryn.module.employee.chat.websocket.EmployeeChatWebSocketHandler employeeChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(portalChatWebSocketHandler, "/ws/portal-chat")
                .setAllowedOriginPatterns("*");
        registry.addHandler(employeeChatWebSocketHandler, "/ws/employee-chat")
                .setAllowedOriginPatterns("*");
    }
}
