package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user suggestions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuggestionResponse {
    private String userId;
    private String userName;
    private String displayName;
    private String bio;
    private String profilePictureUrl;
    private Long followersCount;
    private Long followingCount;
    private Long postsCount;
    private Boolean isFollowing;
    private Double popularityScore; // For sorting
}
