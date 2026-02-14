package com.va.v.v_app.config.websocket;

import com.va.v.v_app.v.service.ChatMessageService;
import com.va.v.v_app.v.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Listens for WebSocket connect/disconnect events.
 *
 * On connect:
 * 1. Register session in memory
 * 2. Mark user ONLINE
 * 3. Push unread/pending messages (DELIVERED status)
 *
 * On disconnect:
 * 1. Remove session
 * 2. If no remaining sessions → mark user OFFLINE
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final PresenceService presenceService;
    private final ChatMessageService chatMessageService;

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (user != null && sessionId != null) {
            String userId = user.getName();
            sessionRegistry.registerSession(userId, sessionId);
            presenceService.userConnected(userId);

            // Deliver pending messages (marks SENT → DELIVERED and pushes)
            try {
                chatMessageService.deliverPendingMessages(userId);
            } catch (Exception e) {
                log.error("Error delivering pending messages for user {}: {}", userId, e.getMessage());
            }

            log.info("WebSocket CONNECTED: userId={}, sessionId={}", userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            String userId = sessionRegistry.removeSession(sessionId);
            if (userId != null && !sessionRegistry.isUserOnline(userId)) {
                presenceService.userDisconnected(userId);
                log.info("WebSocket DISCONNECTED (all sessions): userId={}", userId);
            } else if (userId != null) {
                log.info("WebSocket session closed, user still has other sessions: userId={}", userId);
            }
        }
    }
}
