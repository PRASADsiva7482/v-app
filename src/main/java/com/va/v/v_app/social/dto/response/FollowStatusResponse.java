package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for follow status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusResponse {

    private String userId;
    private Boolean isFollowing;
    private Boolean isFollowedBy; // Does the other user follow you?
    private Boolean isFollowRequestPending; // Is there a pending follow request?
    private Boolean isTargetPrivate; // Is the target user's profile private?
}

