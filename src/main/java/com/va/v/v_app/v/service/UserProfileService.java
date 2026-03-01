package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.UpdateProfileRequest;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.model.UserProfile;
import com.va.v.v_app.v.repository.UserProfileRepository;
import com.va.v.v_app.v.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.va.v.v_app.model.KeycloakAccessToken;

import static com.va.v.v_app.config.CacheConfig.USER_PROFILES_CACHE;

/**
 * Service for managing user profiles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;

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
     * Get user profile by user ID with current user context (CACHED for 1 hour)
     * Auto-creates profile if it doesn't exist
     */
    @Cacheable(value = USER_PROFILES_CACHE, key = "#userId + '-' + (#currentUserId ?: 'anonymous')")
    @Transactional
    public UserProfileResponse getProfileByUserId(String userId, String currentUserId) {
        UserProfile profile = getOrCreateUserProfile(userId);
        return mapToResponse(profile, currentUserId);
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
     * Get user profile by username with current user context
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username, String currentUserId) {
        UserProfile profile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User profile not found for username: " + username));
        return mapToResponse(profile, currentUserId);
    }

    /**
     * Create or get user profile (auto-create if not exists)
     * Uses REQUIRES_NEW to ensure profile creation works even in read-only parent
     * transactions
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
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
     * Uses REQUIRES_NEW to ensure profile creation works even in read-only parent
     * transactions
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public UserProfile getOrCreateUserProfile(String userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("User profile not found for userId: {}. Auto-creating...", userId);

                    String displayName = userId;

                    try {
                        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                                .getRequestAttributes();
                        if (attributes != null && attributes.getRequest() != null) {
                            KeycloakAccessToken tokenDetails = (KeycloakAccessToken) attributes.getRequest()
                                    .getAttribute("KEYCLOAK_TOKEN_DETAILS");
                            if (tokenDetails != null) {
                                if (tokenDetails.getGiven_name() != null
                                        && !tokenDetails.getGiven_name().trim().isEmpty()) {
                                    displayName = tokenDetails.getGiven_name().trim();
                                } else if (tokenDetails.getName() != null && !tokenDetails.getName().trim().isEmpty()) {
                                    displayName = tokenDetails.getName().trim();
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("No request context to extract token details for auto-creating display name");
                    }

                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .username(userId)
                            .displayName(displayName)
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
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName());
        }
        if (request.getUsername() != null) {
            profile.setUsername(request.getUsername());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAbout() != null) {
            profile.setAbout(request.getAbout());
        }
        if (request.getProfilePictureUrl() != null) {
            profile.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        if (request.getCoverPhotoUrl() != null) {
            profile.setCoverPhotoUrl(request.getCoverPhotoUrl());
        }
        if (request.getLocation() != null) {
            profile.setLocation(request.getLocation());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIsPrivate() != null) {
            profile.setIsPrivate(request.getIsPrivate());
        }

        UserProfile updated = userProfileRepository.save(profile);
        log.info("Updated profile for user: {}", userId);
        return mapToResponse(updated, userId); // Pass userId so isOwnProfile is set correctly
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

    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
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
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
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
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
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
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
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
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
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
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
    @Transactional
    public void decrementPostsCount(String userId) {
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setPostsCount(Math.max(0, profile.getPostsCount() - 1));
            userProfileRepository.save(profile);
        });
    }

    /**
     * Search users by username or display name
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserProfileResponse> searchUsers(String keyword,
            org.springframework.data.domain.Pageable pageable) {
        log.debug("Searching users with keyword: {}", keyword);
        org.springframework.data.domain.Page<UserProfile> profiles = userProfileRepository.searchUsers(keyword,
                pageable);
        return profiles.map(this::mapToResponse);
    }

    /**
     * Map entity to response DTO
     */
    private UserProfileResponse mapToResponse(UserProfile profile, String currentUserId) {
        boolean isOwnProfile = currentUserId != null && currentUserId.equals(profile.getUserId());
        boolean isFollowing = false;
        if (currentUserId != null && !isOwnProfile) {
            isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, profile.getUserId());
            log.debug("Follow check: follower={}, following={}, result={}", currentUserId, profile.getUserId(),
                    isFollowing);
        }

        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .about(profile.getAbout())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .coverPhotoUrl(profile.getCoverPhotoUrl())
                .location(profile.getLocation())
                .website(profile.getWebsite())
                .dateOfBirth(profile.getDateOfBirth())
                .phoneNumber(profile.getPhoneNumber())
                .isVerified(profile.getIsVerified())
                .isPrivate(profile.getIsPrivate())
                .followersCount(profile.getFollowersCount())
                .followingCount(profile.getFollowingCount())
                .postsCount(profile.getPostsCount())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .isOwnProfile(isOwnProfile)
                .isFollowing(isFollowing)
                .build();
    }

    /**
     * Map entity to response DTO (backward compatibility - isOwnProfile will be
     * null/false)
     */
    private UserProfileResponse mapToResponse(UserProfile profile) {
        return mapToResponse(profile, null);
    }
}
