package com.va.v.v_app.v.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to send a new chat message (via WebSocket STOMP or REST with media).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    private Long conversationId;
    private String content;
    private String type; // TEXT, IMAGE, VIDEO, AUDIO, FILE
    private Long replyToId; // null if not a reply
    private String tempId; // client-side temporary ID for optimistic UI

    private List<String> mentionedUserIds;

    /**
     * List of attachment info for media messages.
     * Each entry contains fileUrl, fileName, fileType, fileSize.
     */
    private List<AttachmentInfo> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private String fileUrl;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String thumbnailUrl;
    }
}
