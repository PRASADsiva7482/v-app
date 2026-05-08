package com.va.v.v_app.social.api;

import com.va.v.v_app.core.websocket.WebSocketSessionRegistry;
import com.va.v.v_app.social.dto.request.CallSignalingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket STOMP controller for 1:1 Call Signaling (WebRTC).
 *
 * This controller relays SDP offers/answers and ICE candidates
 * between users.
 *
 * Destinations:
 * /app/call.signal → send a signaling event (offers, answers, candidates)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CallWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * Relays call signaling events between users.
     * The backend simply forwards the message to the target user.
     */
    @MessageMapping("/call.signal")
    public void relaySignaling(CallSignalingEvent event, Principal principal) {
        if (principal == null) return;

        String senderId = principal.getName();
        String recipientId = event.getRecipientId();

        if (recipientId == null) {
            log.warn("Signaling attempt without recipientId by {}", senderId);
            return;
        }

        event.setSenderId(senderId);

        if (sessionRegistry.isUserOnline(recipientId)) {
            log.debug("Relaying signaling {} from {} to {}", event.getType(), senderId, recipientId);
            messagingTemplate.convertAndSend("/topic/calls/" + recipientId, event);
        } else {
            log.debug("Recipient {} offline for signaling {}, notifying sender of MISS", recipientId, event.getType());
            if (event.getType() == CallSignalingEvent.Type.CALL_OFFER) {
                // Inform sender that call could not be delivered
                CallSignalingEvent missed = CallSignalingEvent.builder()
                        .type(CallSignalingEvent.Type.CALL_MISSED)
                        .senderId(recipientId)
                        .recipientId(senderId)
                        .isVideo(event.isVideo())
                        .build();
                messagingTemplate.convertAndSend("/topic/calls/" + senderId, missed);
            }
        }
    }
}
