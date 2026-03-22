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
    private String nickname;
    private String bio;
    private String profilePictureUrl;
    private String coverPhotoUrl;
    private String location;
    private String website;
    private Integer followersCount;
    private Integer followingCount;
    private Integer postsCount;
    private String about;
    private LocalDateTime dateOfBirth;
    private String phoneNumber;
    private Boolean isVerified;
    private String verificationTier; // NONE, BLUE, GOLD, GREY
    private Boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for current user context
    private Boolean isFollowing; // Set to true if current user follows this profile
    private Boolean isOwnProfile; // Set to true if this is the current user's profile
}
