package com.va.v.v_app.v.service;

import com.va.v.v_app.config.websocket.WebSocketSessionRegistry;
import com.va.v.v_app.v.dto.request.SendMessageRequest;
import com.va.v.v_app.v.dto.response.MessageResponse;
import com.va.v.v_app.v.dto.response.ReadReceiptEvent;
import com.va.v.v_app.v.model.*;
import com.va.v.v_app.v.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core message service.
 *
 * Flow (exactly as specified):
 * 1. User A sends message via WebSocket
 * 2. Backend validates & persists message
 * 3. Backend checks if User B has active WebSocket session
 * 4. If YES → push message via WebSocket
 * 5. If NO → store message as unread (status = SENT)
 *
 * ─── PHASE-2 NOTE ───
 * When Kafka/RabbitMQ is added:
 * Step 4 changes from direct WebSocket push to:
 * → Publish to message broker topic
 * → WebSocket gateway consumes from broker and pushes to client
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationRepository conversationRepository;
    private final UserProfileRepository userProfileRepository;
    private final WebSocketSessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a message: persist + push to online recipients.
     */
    @Transactional
    public MessageResponse sendMessage(String senderUsername, SendMessageRequest request) {
        // Validate sender is a participant
        if (!participantRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(
                request.getConversationId(), senderUsername)) {
            throw new RuntimeException("User is not a participant of this conversation");
        }

        // Build and persist message
        Message message = Message.builder()
                .conversationId(request.getConversationId())
                .senderId(senderUsername)
                .content(request.getContent())
                .type(request.getType() != null
                        ? Message.MessageType.valueOf(request.getType())
                        : Message.MessageType.TEXT)
                .replyToId(request.getReplyToId())
                .build();
        message = messageRepository.save(message);

        // Update conversation timestamp
        conversationRepository.findById(request.getConversationId())
                .ifPresent(conv -> {
                    conv.setUpdatedAt(LocalDateTime.now());
                    conversationRepository.save(conv);
                });

        // Get all participants except sender
        List<String> recipientUserIds = participantRepository
                .findActiveUserIdsByConversationId(request.getConversationId())
                .stream()
                .filter(uid -> !uid.equals(senderUsername))
                .collect(Collectors.toList());

        // Create message status for each recipient
        for (String recipientId : recipientUserIds) {
            MessageStatus status = MessageStatus.builder()
                    .message(message)
                    .userId(recipientId)
                    .status(MessageStatus.DeliveryStatus.SENT)
                    .build();
            messageStatusRepository.save(status);
        }

        // Build response
        MessageResponse response = toMessageResponse(message, senderUsername);
        response.setTempId(request.getTempId());

        // Push to online recipients via WebSocket
        for (String recipientId : recipientUserIds) {
            if (sessionRegistry.isUserOnline(recipientId)) {
                // User is online → push message AND mark as DELIVERED
                messagingTemplate.convertAndSend(
                        "/topic/messages/" + recipientId, response);

                // Mark as delivered
                MessageStatus ms = messageStatusRepository.findByMessageIdAndUserId(
                        message.getId(), recipientId);
                if (ms != null) {
                    ms.setStatus(MessageStatus.DeliveryStatus.DELIVERED);
                    ms.setDeliveredAt(LocalDateTime.now());
                    messageStatusRepository.save(ms);
                }

                log.debug("Message pushed to online user: {}", recipientId);
            } else {
                log.debug("User {} is offline, message stored as SENT", recipientId);
            }
        }

        // Echo message back to sender (for multi-device support)
        messagingTemplate.convertAndSend("/topic/messages/" + senderUsername, response);

        // Determine outgoing status for sender
        response.setStatus(determineOutgoingStatus(message.getId()));

        log.info("Message sent: id={}, conversation={}, sender={}, recipients={}",
                message.getId(), request.getConversationId(), senderUsername, recipientUserIds.size());

        return response;
    }

    /**
     * Get paginated message history for a conversation.
     * REST API - called only on initial load.
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long conversationId, String userId, int page, int size) {
        // Verify user is participant
        if (!participantRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)) {
            throw new RuntimeException("User is not a participant of this conversation");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository
                .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(conversationId, pageable);

        return messages.map(msg -> toMessageResponse(msg, userId));
    }

    /**
     * Mark all messages in a conversation as READ for a user.
     * Sends read receipt events to message senders.
     */
    @Transactional
    public void markAsRead(Long conversationId, String userId) {
        int updated = messageStatusRepository.markAsRead(conversationId, userId, LocalDateTime.now());

        if (updated > 0) {
            // Update last_read_at for participant
            participantRepository.findByConversationIdAndUserId(conversationId, userId)
                    .ifPresent(p -> {
                        p.setLastReadAt(LocalDateTime.now());
                        participantRepository.save(p);
                    });

            // Send read receipt to other participants
            ReadReceiptEvent event = ReadReceiptEvent.builder()
                    .conversationId(conversationId)
                    .readByUserId(userId)
                    .newStatus("READ")
                    .updatedCount(updated)
                    .build();

            List<String> otherParticipants = participantRepository
                    .findActiveUserIdsByConversationId(conversationId)
                    .stream()
                    .filter(uid -> !uid.equals(userId))
                    .collect(Collectors.toList());

            for (String participantId : otherParticipants) {
                if (sessionRegistry.isUserOnline(participantId)) {
                    messagingTemplate.convertAndSend(
                            "/topic/read-receipt/" + participantId, event);
                }
            }

            log.info("Marked {} messages as READ in conversation {} for user {}",
                    updated, conversationId, userId);
        }
    }

    /**
     * Deliver pending messages when user comes online.
     */
    @Transactional
    public void deliverPendingMessages(String userId) {
        // Find all conversations the user is part of
        List<Conversation> conversations = conversationRepository.findAllByUserId(userId);

        for (Conversation conv : conversations) {
            // Mark all SENT messages as DELIVERED
            int delivered = messageStatusRepository.markAsDelivered(
                    conv.getId(), userId, LocalDateTime.now());

            if (delivered > 0) {
                // Push unread messages to the user
                List<Message> unreadMessages = messageRepository
                        .findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                                conv.getId(), PageRequest.of(0, 50))
                        .getContent();

                for (Message msg : unreadMessages) {
                    if (!msg.getSenderId().equals(userId)) {
                        MessageResponse response = toMessageResponse(msg, userId);
                        messagingTemplate.convertAndSend(
                                "/topic/messages/" + userId, response);
                    }
                }

                // Notify senders about delivery
                ReadReceiptEvent event = ReadReceiptEvent.builder()
                        .conversationId(conv.getId())
                        .readByUserId(userId)
                        .newStatus("DELIVERED")
                        .updatedCount(delivered)
                        .build();

                List<String> otherParticipants = participantRepository
                        .findActiveUserIdsByConversationId(conv.getId())
                        .stream()
                        .filter(uid -> !uid.equals(userId))
                        .collect(Collectors.toList());

                for (String participantId : otherParticipants) {
                    if (sessionRegistry.isUserOnline(participantId)) {
                        messagingTemplate.convertAndSend(
                                "/topic/read-receipt/" + participantId, event);
                    }
                }

                log.info("Delivered {} pending messages to user {} in conversation {}",
                        delivered, userId, conv.getId());
            }
        }
    }

    /**
     * Edit a message.
     */
    @Transactional
    public MessageResponse editMessage(Long messageId, String userId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        message = messageRepository.save(message);

        MessageResponse response = toMessageResponse(message, userId);

        // Notify all participants
        List<String> participantIds = participantRepository
                .findActiveUserIdsByConversationId(message.getConversationId());
        for (String pid : participantIds) {
            if (sessionRegistry.isUserOnline(pid)) {
                messagingTemplate.convertAndSend("/topic/messages/" + pid, response);
            }
        }

        return response;
    }

    /**
     * Delete a message (soft delete).
     */
    @Transactional
    public void deleteMessage(Long messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        message.setIsDeleted(true);
        message.setContent(null);
        messageRepository.save(message);

        MessageResponse response = toMessageResponse(message, userId);

        // Notify all participants
        List<String> participantIds = participantRepository
                .findActiveUserIdsByConversationId(message.getConversationId());
        for (String pid : participantIds) {
            if (sessionRegistry.isUserOnline(pid)) {
                messagingTemplate.convertAndSend("/topic/messages/" + pid, response);
            }
        }
    }

    // ─── Helpers ───

    private MessageResponse toMessageResponse(Message message, String currentUserId) {
        UserProfile senderProfile = userProfileRepository.findByUsername(message.getSenderId())
                .orElse(null);

        String status;
        if (message.getSenderId().equals(currentUserId)) {
            status = determineOutgoingStatus(message.getId());
        } else {
            status = "RECEIVED";
        }

        List<MessageResponse.AttachmentResponse> attachments = Collections.emptyList();
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            attachments = message.getAttachments().stream()
                    .map(a -> MessageResponse.AttachmentResponse.builder()
                            .id(a.getId())
                            .fileUrl(a.getFileUrl())
                            .fileName(a.getFileName())
                            .fileType(a.getFileType())
                            .fileSize(a.getFileSize())
                            .thumbnailUrl(a.getThumbnailUrl())
                            .build())
                    .collect(Collectors.toList());
        }

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderName(senderProfile != null ? senderProfile.getDisplayName() : message.getSenderId())
                .senderAvatar(senderProfile != null ? senderProfile.getProfilePictureUrl() : null)
                .content(message.getIsDeleted() ? null : message.getContent())
                .type(message.getType().name())
                .replyToId(message.getReplyToId())
                .isEdited(message.getIsEdited())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .status(status)
                .attachments(attachments)
                .build();
    }

    private String determineOutgoingStatus(Long messageId) {
        Integer worstStatus = messageStatusRepository.findWorstStatusForMessage(messageId);
        if (worstStatus == null)
            return "SENT";
        return switch (worstStatus) {
            case 0 -> "SENT";
            case 1 -> "DELIVERED";
            case 2 -> "READ";
            default -> "SENT";
        };
    }
}
