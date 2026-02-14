package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for conversation list item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;
    private String type;
    private String groupName;
    private String groupAvatarUrl;

    // For DIRECT conversations - the other participant's info
    private String otherUserId;
    private String otherUserName;
    private String otherUserDisplayName;
    private String otherUserAvatar;
    private Boolean otherUserOnline;

    // Last message preview
    private String lastMessageContent;
    private String lastMessageSenderId;
    private String lastMessageType;
    private LocalDateTime lastMessageTime;

    // Unread count
    private long unreadCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Participants (for group chats)
    private List<ParticipantInfo> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private String userId;
        private String username;
        private String displayName;
        private String avatarUrl;
        private String role;
        private Boolean isOnline;
    }
}
