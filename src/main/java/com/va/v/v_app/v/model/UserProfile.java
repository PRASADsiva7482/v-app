package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a user's social media profile
 */
@Entity
@Table(name = "user_profile", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId; // From Keycloak

    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "cover_photo_url", length = 500)
    private String coverPhotoUrl;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "followers_count", nullable = false)
    @Builder.Default
    private Integer followersCount = 0;

    @Column(name = "following_count", nullable = false)
    @Builder.Default
    private Integer followingCount = 0;

    @Column(name = "posts_count", nullable = false)
    @Builder.Default
    private Integer postsCount = 0;

    @Column(name = "about", columnDefinition = "TEXT")
    private String about; // Detailed about/description section

    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verification_tier", length = 20)
    @Builder.Default
    private String verificationTier = "NONE"; // NONE, BLUE, GOLD, GREY

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "pinned_post_id")
    private Long pinnedPostId;

    @Column(name = "is_ghost_mode", nullable = false)
    @Builder.Default
    private Boolean isGhostMode = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
