package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.UpdateProfileRequest;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.model.UserProfile;
import com.va.v.v_app.v.repository.UserProfileRepository;
import com.va.v.v_app.v.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.va.v.v_app.model.KeycloakAccessToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    @Value("${feature.media.storage.profile-picture-path}")
    private String profilePicturePath;

    @Value("${feature.media.upload.allowed-image-types}")
    private String allowedImageTypes;

    @Value("${feature.media.upload.max-image-size}")
    private long maxImageSize;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    // ==================== Profile Picture Upload ====================

    /**
     * Upload a new profile picture, delete the old one from storage, and update the
     * user profile.
     * Enforces: one user = one profile picture in storage.
     */
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
    @Transactional
    public UserProfileResponse uploadAndUpdateProfilePicture(String userId, MultipartFile file) throws IOException {
        // Validate the file
        validateProfilePictureFile(file);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        // Delete old profile picture from storage if exists
        deleteOldProfilePictureFile(profile);

        // Generate unique filename and save the new file
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        String uniqueFilename = generateProfilePicFilename(userId, extension);
        Path targetPath = Paths.get(profilePicturePath, uniqueFilename);

        // Ensure directory exists
        Files.createDirectories(targetPath.getParent());

        // Save the new file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Build URL for serving
        String basePath = contextPath.isEmpty() ? "" : contextPath;
        String fileUrl = String.format("%s/api/v1/media/profile-pictures/%s", basePath, uniqueFilename);

        // Update profile
        profile.setProfilePictureUrl(fileUrl);
        UserProfile updated = userProfileRepository.save(profile);

        log.info("✅ Profile picture updated for user: {} -> {}", userId, fileUrl);
        return mapToResponse(updated, userId);
    }

    /**
     * Delete the current profile picture from storage and clear URL from profile.
     */
    @CacheEvict(value = USER_PROFILES_CACHE, allEntries = true)
    @Transactional
    public UserProfileResponse deleteProfilePicture(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        deleteOldProfilePictureFile(profile);

        profile.setProfilePictureUrl(null);
        UserProfile updated = userProfileRepository.save(profile);

        log.info("✅ Profile picture deleted for user: {}", userId);
        return mapToResponse(updated, userId);
    }

    /**
     * Delete old profile picture file from disk if it exists
     */
    private void deleteOldProfilePictureFile(UserProfile profile) {
        String oldPictureUrl = profile.getProfilePictureUrl();
        if (oldPictureUrl != null && !oldPictureUrl.isEmpty() && oldPictureUrl.contains("/profile-pictures/")) {
            try {
                // Extract filename from URL: .../profile-pictures/filename.ext
                String oldFilename = oldPictureUrl.substring(oldPictureUrl.lastIndexOf("/") + 1);
                Path oldFilePath = Paths.get(profilePicturePath, oldFilename);
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                    log.info("🗑️ Deleted old profile picture: {}", oldFilePath);
                }
            } catch (IOException e) {
                log.warn("⚠️ Failed to delete old profile picture: {}", oldPictureUrl, e);
            }
        }
    }

    private void validateProfilePictureFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile picture file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Filename is invalid");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedImages = Arrays.asList(allowedImageTypes.split(","));
        if (!allowedImages.contains(extension)) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + extension + ". Allowed: " + allowedImageTypes);
        }

        if (file.getSize() > maxImageSize) {
            throw new IllegalArgumentException("Profile picture size exceeds maximum allowed size of " +
                    (maxImageSize / 1024 / 1024) + "MB");
        }
    }

    private String generateProfilePicFilename(String userId, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("pfp_%s_%s_%s.%s", userId, timestamp, uuid, extension);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1)
            return "";
        return filename.substring(lastDotIndex + 1);
    }

    // ==================== Original Profile Methods ====================

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
        if (request.getNickname() != null) {
            profile.setNickname(request.getNickname());
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
                .nickname(profile.getNickname())
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
