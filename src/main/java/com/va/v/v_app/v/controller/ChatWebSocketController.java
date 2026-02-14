package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.request.MarkReadRequest;
import com.va.v.v_app.v.dto.request.SendMessageRequest;
import com.va.v.v_app.v.dto.response.MessageResponse;
import com.va.v.v_app.v.dto.response.TypingEvent;
import com.va.v.v_app.v.service.ChatMessageService;
import com.va.v.v_app.config.websocket.WebSocketSessionRegistry;
import com.va.v.v_app.v.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket STOMP controller for real-time chat.
 *
 * Destinations:
 * /app/chat.send → send a message
 * /app/chat.read → mark messages as read
 * /app/chat.typing → typing indicator
 *
 * These are the ONLY real-time channels.
 * 🚫 NO polling
 * 🚫 NO cron jobs
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;
    private final ConversationParticipantRepository participantRepository;

    /**
     * Handle incoming chat message from client.
     *
     * Flow:
     * Client sends to /app/chat.send
     * → Backend validates & persists
     * → Backend pushes to /topic/messages/{recipientId}
     * → If recipient offline, message stays as SENT in DB
     */
    @MessageMapping("/chat.send")
    public void sendMessage(SendMessageRequest request, Principal principal) {
        if (principal == null) {
            log.warn("Unauthenticated WebSocket message attempt");
            return;
        }

        String senderUsername = principal.getName();
        log.info("WS message received from {}: conv={}", senderUsername, request.getConversationId());

        try {
            chatMessageService.sendMessage(senderUsername, request);
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle read receipt from client.
     *
     * Client sends to /app/chat.read when conversation is viewed
     * → Backend marks all messages as READ
     * → Backend pushes read receipt to sender(s)
     */
    @MessageMapping("/chat.read")
    public void markAsRead(MarkReadRequest request, Principal principal) {
        if (principal == null)
            return;

        String userId = principal.getName();
        log.debug("WS read receipt: user={}, conv={}", userId, request.getConversationId());

        try {
            chatMessageService.markAsRead(request.getConversationId(), userId);
        } catch (Exception e) {
            log.error("Error marking messages as read: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle typing indicator from client.
     *
     * Client sends to /app/chat.typing
     * → Backend broadcasts to other participants in the conversation
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(TypingEvent event, Principal principal) {
        if (principal == null)
            return;

        String userId = principal.getName();
        event.setUserId(userId);

        // Get other participants and send typing event
        List<String> recipients = participantRepository
                .findActiveUserIdsByConversationId(event.getConversationId())
                .stream()
                .filter(uid -> !uid.equals(userId))
                .collect(Collectors.toList());

        for (String recipientId : recipients) {
            if (sessionRegistry.isUserOnline(recipientId)) {
                messagingTemplate.convertAndSend(
                        "/topic/typing/" + event.getConversationId(), event);
                break; // Same topic for all in conversation
            }
        }
    }
}
