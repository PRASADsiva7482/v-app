package com.va.v.v_app.social.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebRTC Signaling message for video/audio calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSignalingEvent {
    public enum Type {
        CALL_OFFER,
        CALL_ANSWER,
        ICE_CANDIDATE,
        CALL_REJECTED,
        CALL_ENDED,
        CALL_MISSED,
        CALL_RINGING
    }

    private Type type;
    private String senderId;
    private String recipientId;
    private String conversationId;
    private Object payload; // SDP data or ICE candidate
    private boolean isVideo;
}
