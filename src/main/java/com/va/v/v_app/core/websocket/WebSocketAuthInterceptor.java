package com.va.v.v_app.core.websocket;

import com.va.v.v_app.core.security.KeycloakTokenValidator;
import com.va.v.v_app.iam.model.KeycloakAccessToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;
import java.util.List;

/**
 * WebSocket STOMP authentication interceptor — HARDENED.
 *
 * Security enforcement:
 *   ✅ Requires valid JWT on CONNECT (rejects unauthenticated)
 *   ✅ No static secrets (removed X-V2-App-Secret — visible in DevTools)
 *   ✅ WebSocket origin is already restricted in WebSocketConfig
 *   ✅ Connection rejected outright if no valid token
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketAuthInterceptor implements WebSocketMessageBrokerConfigurer {

    private final KeycloakTokenValidator keycloakTokenValidator;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = extractToken(accessor);

                    if (token == null) {
                        log.warn("WebSocket CONNECT rejected: no token provided");
                        return null; // Reject connection
                    }

                    try {
                        KeycloakAccessToken tokenDetails = keycloakTokenValidator.validateToken(token);

                        if (tokenDetails == null || !"true".equalsIgnoreCase(tokenDetails.getActive())) {
                            log.warn("WebSocket CONNECT rejected: invalid or expired token");
                            return null; // Reject connection
                        }

                        // Extract username (same resolution as JwtRequestFilter)
                        String username = tokenDetails.getPreferred_username();
                        if (username == null || username.isEmpty()) {
                            username = tokenDetails.getUsername();
                        }
                        if (username == null || username.isEmpty()) {
                            username = tokenDetails.getSub();
                        }

                        if (username == null || username.isEmpty()) {
                            log.warn("WebSocket CONNECT rejected: could not extract username");
                            return null; // Reject connection
                        }

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                username, null, Collections.emptyList());
                        accessor.setUser(auth);
                        log.info("WebSocket CONNECT authenticated: user={}", username);

                    } catch (Exception e) {
                        log.error("WebSocket CONNECT authentication failed");
                        return null; // Reject connection — don't log e.getMessage()
                    }
                }

                return message;
            }
        });
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }

        // Try dedicated 'token' header as fallback
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }
}
