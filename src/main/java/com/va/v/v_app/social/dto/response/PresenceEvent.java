package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket event for presence changes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceEvent {

    private String userId;
    private boolean online;
    private LocalDateTime lastSeenAt;
}
