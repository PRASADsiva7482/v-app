package com.va.v.v_app.v.dto.response;

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
}
