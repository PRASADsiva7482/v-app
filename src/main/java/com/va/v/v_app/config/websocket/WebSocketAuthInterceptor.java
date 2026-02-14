package com.va.v.v_app.config.websocket;

import com.va.v.v_app.config.security.KeycloakTokenValidator;
import com.va.v.v_app.model.KeycloakAccessToken;
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
 * Intercept WebSocket CONNECT frames to authenticate via JWT token.
 * The token is passed as a STOMP header: Authorization: Bearer xxx
 *
 * The principal name is set to the Keycloak preferred_username (same as
 * what the REST JwtRequestFilter uses in SecurityContext).
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

                    if (token != null) {
                        try {
                            KeycloakAccessToken tokenDetails = keycloakTokenValidator.validateToken(token);

                            if (tokenDetails != null && "true".equalsIgnoreCase(tokenDetails.getActive())) {
                                // Use same principal resolution as JwtRequestFilter
                                String username = tokenDetails.getPreferred_username();
                                if (username == null || username.isEmpty()) {
                                    username = tokenDetails.getUsername();
                                }
                                if (username == null || username.isEmpty()) {
                                    username = tokenDetails.getSub();
                                }

                                if (username != null && !username.isEmpty()) {
                                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                            username, null, Collections.emptyList());
                                    accessor.setUser(auth);
                                    log.info("WebSocket CONNECT authenticated: user={}", username);
                                } else {
                                    log.warn("WebSocket CONNECT: could not extract username from token");
                                }
                            } else {
                                log.warn("WebSocket CONNECT: token validation failed or inactive");
                            }
                        } catch (Exception e) {
                            log.error("WebSocket CONNECT authentication failed: {}", e.getMessage());
                        }
                    } else {
                        log.warn("WebSocket CONNECT: no token found in headers");
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
