package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for user profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String userId;
    private String username;
    private String displayName;
    private String bio;
    private String profilePictureUrl;
    private String coverPhotoUrl;
    private String location;
    private String website;
    private Integer followersCount;
    private Integer followingCount;
    private Integer postsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional field for current user context
    private Boolean isFollowing; // Set to true if current user follows this profile
}
