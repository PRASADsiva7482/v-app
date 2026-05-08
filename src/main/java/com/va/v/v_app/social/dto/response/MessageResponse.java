package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a chat message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private Long conversationId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String type; // TEXT, IMAGE, etc.
    private Long replyToId;
    private Boolean isEdited;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private String tempId; // echoed back for optimistic UI matching

    // Delivery status: SENT, DELIVERED, READ
    private String status;

    // Attachments
    private List<AttachmentResponse> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentResponse {
        private Long id;
        private String fileUrl;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String thumbnailUrl;
    }
}
