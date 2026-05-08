package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket event for typing indicator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {

    private Long conversationId;
    private String userId;
    private String username;
    private boolean typing;
}
