package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String recipientId;
    private String senderId;
    private String type; // LIKE, COMMENT, FOLLOW, MENTION, REPOST, REPLY, SYSTEM
    private String message;
    private Long referenceId;
    private String referenceType; // POST, COMMENT, USER
    private Boolean isRead;
    private Boolean isSeen;
    private LocalDateTime createdAt;

    // Enriched sender info
    private String senderUsername;
    private String senderDisplayName;
    private String senderProfilePictureUrl;

    // Enriched reference info (post content preview, etc.)
    private String referenceContent;
}
