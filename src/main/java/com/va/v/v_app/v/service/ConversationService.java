package com.va.v.v_app.v.service;

import com.va.v.v_app.config.websocket.WebSocketSessionRegistry;
import com.va.v.v_app.v.dto.request.StartConversationRequest;
import com.va.v.v_app.v.dto.response.ConversationResponse;
import com.va.v.v_app.v.model.Conversation;
import com.va.v.v_app.v.model.ConversationParticipant;
import com.va.v.v_app.v.model.Message;
import com.va.v.v_app.v.model.UserProfile;
import com.va.v.v_app.v.repository.*;
import com.va.v.v_app.v.exception.BusinessException;
import com.va.v.v_app.v.exception.ResourceNotFoundException;
import com.va.v.v_app.v.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages conversations (chat threads).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

        private final ConversationRepository conversationRepository;
        private final ConversationParticipantRepository participantRepository;
        private final MessageRepository messageRepository;
        private final UserProfileRepository userProfileRepository;
        private final WebSocketSessionRegistry sessionRegistry;

        /**
         * Get all conversations for the current user.
         * REST API - called only once on initial load.
         */
        @Transactional(readOnly = true)
        public List<ConversationResponse> getConversations(String userId) {
                List<Conversation> conversations = conversationRepository.findAllByUserId(userId);

                return conversations.stream()
                                .map(conv -> toConversationResponse(conv, userId))
                                .collect(Collectors.toList());
        }

        /**
         * Start a new 1:1 conversation, or return existing one.
         */
        @Transactional
        public ConversationResponse startDirectConversation(String currentUserId, StartConversationRequest request) {
                String recipientId = request.getRecipientUserId();

                if (currentUserId.equals(recipientId)) {
                        throw new BusinessException("SELF_CONVERSATION", "Cannot start a conversation with yourself");
                }

                // Check if conversation already exists
                Optional<Conversation> existing = conversationRepository
                                .findDirectConversation(currentUserId, recipientId);
                if (existing.isPresent()) {
                        return toConversationResponse(existing.get(), currentUserId);
                }

                // Create new conversation
                Conversation conversation = Conversation.builder()
                                .type(Conversation.ConversationType.DIRECT)
                                .createdBy(currentUserId)
                                .participants(new ArrayList<>())
                                .build();
                conversation = conversationRepository.save(conversation);

                // Add participants
                ConversationParticipant p1 = ConversationParticipant.builder()
                                .conversation(conversation)
                                .userId(currentUserId)
                                .role(ConversationParticipant.ParticipantRole.OWNER)
                                .build();

                ConversationParticipant p2 = ConversationParticipant.builder()
                                .conversation(conversation)
                                .userId(recipientId)
                                .role(ConversationParticipant.ParticipantRole.MEMBER)
                                .build();

                participantRepository.save(p1);
                participantRepository.save(p2);

                conversation.getParticipants().add(p1);
                conversation.getParticipants().add(p2);

                log.info("New DIRECT conversation created: id={}, between {} and {}",
                                conversation.getId(), currentUserId, recipientId);

                return toConversationResponse(conversation, currentUserId);
        }

        /**
         * Get a single conversation by ID.
         */
        @Transactional(readOnly = true)
        public ConversationResponse getConversation(Long conversationId, String userId) {
                Conversation conversation = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

                if (!participantRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)) {
                        throw new UnauthorizedException("You are not a participant of this conversation");
                }

                return toConversationResponse(conversation, userId);
        }

        // ─── Mappers ───

        private ConversationResponse toConversationResponse(Conversation conversation, String currentUserId) {
                ConversationResponse.ConversationResponseBuilder builder = ConversationResponse.builder()
                                .id(conversation.getId())
                                .type(conversation.getType().name())
                                .groupName(conversation.getGroupName())
                                .groupAvatarUrl(conversation.getGroupAvatarUrl())
                                .createdAt(conversation.getCreatedAt())
                                .updatedAt(conversation.getUpdatedAt());

                // Get participants
                List<ConversationParticipant> participants = participantRepository
                                .findByConversationIdAndLeftAtIsNull(conversation.getId());

                List<ConversationResponse.ParticipantInfo> participantInfos = participants.stream()
                                .map(p -> {
                                        UserProfile profile = userProfileRepository.findByUsername(p.getUserId())
                                                        .orElse(null);
                                        return ConversationResponse.ParticipantInfo.builder()
                                                        .userId(p.getUserId())
                                                        .username(profile != null ? profile.getUsername()
                                                                        : p.getUserId())
                                                        .displayName(profile != null ? profile.getDisplayName()
                                                                        : p.getUserId())
                                                        .avatarUrl(profile != null ? profile.getProfilePictureUrl()
                                                                        : null)
                                                        .role(p.getRole().name())
                                                        .isOnline(sessionRegistry.isUserOnline(p.getUserId()))
                                                        .build();
                                })
                                .collect(Collectors.toList());
                builder.participants(participantInfos);

                // For DIRECT conversations, populate other user info
                if (conversation.getType() == Conversation.ConversationType.DIRECT) {
                        participantInfos.stream()
                                        .filter(p -> !p.getUserId().equals(currentUserId))
                                        .findFirst()
                                        .ifPresent(other -> {
                                                builder.otherUserId(other.getUserId());
                                                builder.otherUserName(other.getUsername());
                                                builder.otherUserDisplayName(other.getDisplayName());
                                                builder.otherUserAvatar(other.getAvatarUrl());
                                                builder.otherUserOnline(other.getIsOnline());
                                        });
                }

                // Last message
                messageRepository.findTopByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(conversation.getId())
                                .ifPresent(msg -> {
                                        builder.lastMessageContent(msg.getContent());
                                        builder.lastMessageSenderId(msg.getSenderId());
                                        builder.lastMessageType(msg.getType().name());
                                        builder.lastMessageTime(msg.getCreatedAt());
                                });

                // Unread count
                long unread = messageRepository.countUnreadMessages(conversation.getId(), currentUserId);
                builder.unreadCount(unread);

                return builder.build();
        }

        /**
         * Create a GROUP conversation with multiple participants.
         */
        @Transactional
        public ConversationResponse createGroupConversation(String currentUserId, StartConversationRequest request) {
                Conversation conversation = Conversation.builder()
                                .type(Conversation.ConversationType.GROUP)
                                .groupName(request.getGroupName() != null ? request.getGroupName() : "Group Chat")
                                .groupAvatarUrl(request.getGroupAvatarUrl())
                                .createdBy(currentUserId)
                                .participants(new ArrayList<>())
                                .build();
                conversation = conversationRepository.save(conversation);

                // Add creator as OWNER
                ConversationParticipant owner = ConversationParticipant.builder()
                                .conversation(conversation)
                                .userId(currentUserId)
                                .role(ConversationParticipant.ParticipantRole.OWNER)
                                .build();
                participantRepository.save(owner);
                conversation.getParticipants().add(owner);

                // Add other participants
                if (request.getParticipantIds() != null) {
                        for (String memberId : request.getParticipantIds()) {
                                if (!memberId.equals(currentUserId)) {
                                        ConversationParticipant member = ConversationParticipant.builder()
                                                        .conversation(conversation)
                                                        .userId(memberId)
                                                        .role(ConversationParticipant.ParticipantRole.MEMBER)
                                                        .build();
                                        participantRepository.save(member);
                                        conversation.getParticipants().add(member);
                                }
                        }
                }

                log.info("New GROUP conversation created: id={}, name={}, members={}",
                                conversation.getId(), conversation.getGroupName(),
                                conversation.getParticipants().size());

                return toConversationResponse(conversation, currentUserId);
        }

        /**
         * Add a member to a GROUP conversation.
         */
        @Transactional
        public ConversationResponse addGroupMember(Long conversationId, String requesterId, String newMemberId) {
                Conversation conversation = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

                if (conversation.getType() != Conversation.ConversationType.GROUP) {
                        throw new BusinessException("NOT_GROUP", "Can only add members to group conversations");
                }

                // Check requester is a participant
                if (!participantRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, requesterId)) {
                        throw new UnauthorizedException("You are not a participant of this conversation");
                }

                // Check if already a member
                if (participantRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, newMemberId)) {
                        throw new BusinessException("ALREADY_MEMBER", "User is already a member of this group");
                }

                ConversationParticipant member = ConversationParticipant.builder()
                                .conversation(conversation)
                                .userId(newMemberId)
                                .role(ConversationParticipant.ParticipantRole.MEMBER)
                                .build();
                participantRepository.save(member);

                return toConversationResponse(conversation, requesterId);
        }

        /**
         * Remove a member from a GROUP conversation.
         */
        @Transactional
        public void removeGroupMember(Long conversationId, String requesterId, String memberId) {
                Conversation conversation = conversationRepository.findById(conversationId)
                                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

                if (conversation.getType() != Conversation.ConversationType.GROUP) {
                        throw new BusinessException("NOT_GROUP", "Can only remove members from group conversations");
                }

                participantRepository.findByConversationIdAndUserId(conversationId, memberId)
                                .ifPresent(p -> {
                                        p.setLeftAt(java.time.LocalDateTime.now());
                                        participantRepository.save(p);
                                });
        }
}
