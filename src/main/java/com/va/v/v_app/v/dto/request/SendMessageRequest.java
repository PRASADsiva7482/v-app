package com.va.v.v_app.v.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to send a new chat message (via WebSocket STOMP)
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
}
