package com.va.v.v_app.config.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket / STOMP configuration.
 *
 * Endpoints:
 * /ws (raw WebSocket — used by native WebSocket / @stomp/stompjs)
 * /ws-sockjs (SockJS fallback — for legacy browsers)
 *
 * Topic layout:
 * /topic/messages/{userId} → new messages pushed to user
 * /topic/presence → online/offline events
 * /topic/read-receipt/{userId} → read receipt updates
 * /topic/typing/{conversationId} → typing indicators
 *
 * App destination prefix: /app
 * /app/chat.send → send a message
 * /app/chat.read → mark messages as read
 * /app/chat.typing → typing indicator
 *
 * ─── PHASE-2 NOTE ───
 * When Kafka/RabbitMQ is added, replace the simple in-memory broker
 * with an external broker relay (e.g., enableStompBrokerRelay).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple in-memory broker for Phase-1
        // PHASE-2: Replace with config.enableStompBrokerRelay("/topic", "/queue")
        // .setRelayHost("localhost").setRelayPort(61613);
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");

        // Raw WebSocket endpoint (for modern browsers / @stomp/stompjs with native WS)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins);

        // SockJS fallback endpoint (for legacy browser support)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOrigins(origins)
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024); // 128 KB
        registration.setSendBufferSizeLimit(512 * 1024); // 512 KB
        registration.setSendTimeLimit(20_000); // 20 seconds
    }
}
