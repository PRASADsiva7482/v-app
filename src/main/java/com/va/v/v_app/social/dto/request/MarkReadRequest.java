package com.va.v.v_app.social.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to mark messages as read (via WebSocket)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkReadRequest {

    private Long conversationId;
}
