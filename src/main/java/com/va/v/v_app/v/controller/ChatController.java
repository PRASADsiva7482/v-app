package com.va.v.v_app.v.controller;

import com.va.v.v_app.config.security.SecurityContextUtil;
import com.va.v.v_app.v.dto.request.StartConversationRequest;
import com.va.v.v_app.v.dto.response.ConversationResponse;
import com.va.v.v_app.v.dto.response.MessageResponse;
import com.va.v.v_app.v.service.ChatMessageService;
import com.va.v.v_app.v.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Chat.
 *
 * Purpose | API | Technology
 * ────────────────────┼────────────────────────────────────────────┼───────────
 * Initial chat list │ GET /api/v1/chat/conversations │ REST (single call)
 * Start conversation │ POST /api/v1/chat/conversations │ REST
 * Message history │ GET /api/v1/chat/messages/{convId} │ REST (pagination)
 * Edit message │ PUT /api/v1/chat/messages/{msgId} │ REST
 * Delete message │ DELETE /api/v1/chat/messages/{msgId} │ REST
 *
 * 🚫 NO polling APIs
 * 🚫 NO "check for updates" APIs
 *
 * Real-time delivery → WebSocket (see ChatWebSocketController)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final ChatMessageService chatMessageService;

    /**
     * Get all conversations for the current user.
     * Called ONCE on initial load.
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations() {
        String userId = SecurityContextUtil.getCurrentUsername();
        List<ConversationResponse> conversations = conversationService.getConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    /**
     * Get a specific conversation by ID.
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable Long conversationId) {
        String userId = SecurityContextUtil.getCurrentUsername();
        ConversationResponse conversation = conversationService.getConversation(conversationId, userId);
        return ResponseEntity.ok(conversation);
    }

    /**
     * Start a new 1:1 conversation (or return existing).
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            @RequestBody StartConversationRequest request) {
        String userId = SecurityContextUtil.getCurrentUsername();
        ConversationResponse conversation = conversationService.startDirectConversation(userId, request);
        return ResponseEntity.ok(conversation);
    }

    /**
     * Get paginated message history.
     * Called ONCE per conversation on open (with pagination for scroll-up).
     */
    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        String userId = SecurityContextUtil.getCurrentUsername();
        Page<MessageResponse> messages = chatMessageService.getMessages(conversationId, userId, page, size);
        return ResponseEntity.ok(messages);
    }

    /**
     * Edit a message.
     */
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent) {
        String userId = SecurityContextUtil.getCurrentUsername();
        MessageResponse response = chatMessageService.editMessage(messageId, userId, newContent);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a message (soft delete).
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        String userId = SecurityContextUtil.getCurrentUsername();
        chatMessageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }
}
