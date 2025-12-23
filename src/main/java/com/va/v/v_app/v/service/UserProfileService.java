package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.UpdateProfileRequest;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.model.UserProfile;
import com.va.v.v_app.v.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.va.v.v_app.config.CacheConfig.USER_PROFILES_CACHE;

/**
 * Service for managing user profiles
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /**
     * Get user profile by user ID (CACHED for 1 hour)
     * Auto-creates profile if it doesn't exist
     */
    @Cacheable(value = USER_PROFILES_CACHE, key = "#userId")
    @Transactional
    public UserProfileResponse getProfileByUserId(String userId) {
        UserProfile profile = getOrCreateUserProfile(userId);
        return mapToResponse(profile);
    }

    /**
     * Get user profile by username
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username) {
        UserProfile profile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User profile not found for username: " + username));
        return mapToResponse(profile);
    }

    /**
     * Create or get user profile (auto-create if not exists)
     */
    @Transactional
    public UserProfile getOrCreateProfile(String userId, String username) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new profile for user: {}", userId);
                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .username(username != null ? username : "user_" + userId)
                            .displayName(username)
                            .build();
                    return userProfileRepository.save(newProfile);
                });
    }

    /**
     * Get or auto-create user profile by userId only
     * Used when we only have userId (e.g., from JWT token)
     */
    @Transactional
    public UserProfile getOrCreateUserProfile(String userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("User profile not found for userId: {}. Auto-creating...", userId);

                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .username(userId)
                            .displayName("User " + userId.substring(0, Math.min(8, userId.length())))
                            .bio("")
                            .followersCount(0)
                            .followingCount(0)
                            .postsCount(0)
                            .build();

                    UserProfile saved = userProfileRepository.save(newProfile);
                    log.info("✅ Auto-created user profile for userId: {}", userId);

                    return saved;
                });
    }

    /**
     * Update user profile (Evicts cache)
     */
    @CacheEvict(value = USER_PROFILES_CACHE, key = "#userId")
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }

        UserProfile updated = userProfileRepository.save(profile);
        log.info("Updated profile for user: {}", userId);
        return mapToResponse(updated);
    }

    /**
     * Update profile picture
     */
    @Transactional
    public UserProfileResponse updateProfilePicture(String userId, String pictureUrl) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        profile.setProfilePictureUrl(pictureUrl);
        UserProfile updated = userProfileRepository.save(profile);
        return mapToResponse(updated);
    }

    /**
     * Update cover photo
     */
    @Transactional
    public UserProfileResponse updateCoverPhoto(String userId, String coverPhotoUrl) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        profile.setCoverPhotoUrl(coverPhotoUrl);
        UserProfile updated = userProfileRepository.save(profile);
        return mapToResponse(updated);
    }

    /**
     * Increment followers count
     */
    @Transactional
    public void incrementFollowersCount(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setFollowersCount(profile.getFollowersCount() + 1);
            userProfileRepository.save(profile);
        });
    }

    /**
     * Decrement followers count
     */
    @Transactional
    public void decrementFollowersCount(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setFollowersCount(Math.max(0, profile.getFollowersCount() - 1));
            userProfileRepository.save(profile);
        });
    }

    /**
     * Increment following count
     */
    @Transactional
    public void incrementFollowingCount(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setFollowingCount(profile.getFollowingCount() + 1);
            userProfileRepository.save(profile);
        });
    }

    /**
     * Decrement following count
     */
    @Transactional
    public void decrementFollowingCount(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setFollowingCount(Math.max(0, profile.getFollowingCount() - 1));
            userProfileRepository.save(profile);
        });
    }

    /**
     * Increment posts count
     */
    @Transactional
    public void incrementPostsCount(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setPostsCount(profile.getPostsCount() + 1);
            userProfileRepository.save(profile);
        });
    }

    /**
     * Decrement posts count
     */
    @Transactional
    public void decrementPostsCount(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setPostsCount(Math.max(0, profile.getPostsCount() - 1));
            userProfileRepository.save(profile);
        });
    }

    /**
     * Map entity to response DTO
     */
    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .coverPhotoUrl(profile.getCoverPhotoUrl())
                .location(profile.getLocation())
                .website(profile.getWebsite())
                .followersCount(profile.getFollowersCount())
                .followingCount(profile.getFollowingCount())
                .postsCount(profile.getPostsCount())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
