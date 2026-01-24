package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for smart user suggestions based on network
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartSuggestionResponse {
    private String userId;
    private String userName;
    private String displayName;
    private String bio;
    private String profilePictureUrl;
    private Long followersCount;
    private Long followingCount;
    private Long postsCount;
    private Boolean isFollowing;

    // Additional context fields
    private Integer mutualFollowersCount; // How many of your followers also follow this user
    private List<String> mutualFollowerNames; // Names of mutual followers (max 3 for display)
    private String suggestionReason; // e.g., "Followed by john_doe and 2 others"
    private Double relevanceScore; // For sorting
}
