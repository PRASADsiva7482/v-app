package com.va.v.v_app.social.service;

import com.va.v.v_app.social.dto.UserSettingsDto;
import com.va.v.v_app.social.model.UserSettings;
import com.va.v.v_app.social.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingsService {

    private final UserSettingsRepository settingsRepository;
    private final KeycloakAdminService keycloakAdminService;

    // All repositories needed for complete account deletion
    private final UserProfileRepository userProfileRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final FollowRepository followRepository;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final UserPresenceRepository userPresenceRepository;

    /**
     * Get user settings, or create default if they don't exist
     */
    @Transactional
    public UserSettingsDto getUserSettings(String userId) {
        UserSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return mapToDto(settings);
    }

    /**
     * Update user settings. Only non-null fields will be updated.
     */
    @Transactional
    public UserSettingsDto updateUserSettings(String userId, UserSettingsDto updateDto) {
        UserSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        // Security & Account Access
        if (updateDto.getTwoFactorAuth() != null)
            settings.setTwoFactorAuth(updateDto.getTwoFactorAuth());
        if (updateDto.getPasswordProtection() != null)
            settings.setPasswordProtection(updateDto.getPasswordProtection());
        if (updateDto.getAppSessionsTracking() != null)
            settings.setAppSessionsTracking(updateDto.getAppSessionsTracking());

        // Privacy & Safety
        if (updateDto.getProtectPosts() != null)
            settings.setProtectPosts(updateDto.getProtectPosts());
        if (updateDto.getPhotoTagging() != null)
            settings.setPhotoTagging(updateDto.getPhotoTagging());
        if (updateDto.getLocationInfo() != null)
            settings.setLocationInfo(updateDto.getLocationInfo());
        if (updateDto.getSensitiveMedia() != null)
            settings.setSensitiveMedia(updateDto.getSensitiveMedia());
        if (updateDto.getDirectMessagePrivacy() != null)
            settings.setDirectMessagePrivacy(updateDto.getDirectMessagePrivacy());
        if (updateDto.getReadReceipts() != null)
            settings.setReadReceipts(updateDto.getReadReceipts());
        if (updateDto.getDiscoverableByEmail() != null)
            settings.setDiscoverableByEmail(updateDto.getDiscoverableByEmail());
        if (updateDto.getDiscoverableByPhone() != null)
            settings.setDiscoverableByPhone(updateDto.getDiscoverableByPhone());
        if (updateDto.getAllowPersonalizedAds() != null)
            settings.setAllowPersonalizedAds(updateDto.getAllowPersonalizedAds());
        if (updateDto.getAllowDataSharing() != null)
            settings.setAllowDataSharing(updateDto.getAllowDataSharing());

        // Notifications
        if (updateDto.getQualityFilter() != null)
            settings.setQualityFilter(updateDto.getQualityFilter());
        if (updateDto.getMuteAccountsNotFollowing() != null)
            settings.setMuteAccountsNotFollowing(updateDto.getMuteAccountsNotFollowing());
        if (updateDto.getMuteAccountsNew() != null)
            settings.setMuteAccountsNew(updateDto.getMuteAccountsNew());
        if (updateDto.getPushNotifications() != null)
            settings.setPushNotifications(updateDto.getPushNotifications());
        if (updateDto.getEmailNotifications() != null)
            settings.setEmailNotifications(updateDto.getEmailNotifications());
        if (updateDto.getSmsNotifications() != null)
            settings.setSmsNotifications(updateDto.getSmsNotifications());

        // Accessibility, display, and languages
        if (updateDto.getDisplayLanguage() != null)
            settings.setDisplayLanguage(updateDto.getDisplayLanguage());
        if (updateDto.getAppearanceTheme() != null)
            settings.setAppearanceTheme(updateDto.getAppearanceTheme());
        if (updateDto.getFontSize() != null)
            settings.setFontSize(updateDto.getFontSize());
        if (updateDto.getReduceMotion() != null)
            settings.setReduceMotion(updateDto.getReduceMotion());
        if (updateDto.getDataSaver() != null)
            settings.setDataSaver(updateDto.getDataSaver());

        settings = settingsRepository.save(settings);
        return mapToDto(settings);
    }

    /**
     * Complete Account Deletion Flow.
     * Deletes ALL user data from the local database, then removes the identity
     * from Keycloak so the user can never log in again.
     *
     * Deletion order matters due to foreign key constraints:
     * 1. Message statuses (references messages)
     * 2. Messages (sent by user)
     * 3. Conversation participants
     * 4. Comment likes (by user)
     * 5. Comments (by user, on any post)
     * 6. Post likes (by user)
     * 7. Posts (by user — cascade deletes their media, comments, post-hashtag
     * links)
     * 8. Follows (both directions)
     * 9. Notifications (both sent and received)
     * 10. User profile
     * 11. User settings
     * 12. User presence
     * 13. Keycloak identity
     */
    @Transactional
    public boolean deleteUserAccountEntirely(String userId) {
        log.warn("Executing FULL deletion of account for user {}", userId);

        try {
            // 1. Delete message statuses (must come before messages due to FK)
            log.info("Deleting message statuses for user {}", userId);
            messageStatusRepository.deleteByUserId(userId);

            // 2. Delete messages sent by the user
            log.info("Deleting messages for user {}", userId);
            messageRepository.deleteBySenderId(userId);

            // 3. Remove user from conversations
            log.info("Deleting conversation participations for user {}", userId);
            conversationParticipantRepository.deleteByUserId(userId);

            // 4. Delete comment likes by the user
            log.info("Deleting comment likes for user {}", userId);
            commentLikeRepository.deleteByUserId(userId);

            // 5. Delete comments made by the user (on any post)
            log.info("Deleting comments for user {}", userId);
            commentRepository.deleteByUserId(userId);

            // 6. Delete post likes by the user
            log.info("Deleting post likes for user {}", userId);
            postLikeRepository.deleteByUserId(userId);

            // 7. Delete all posts by the user (cascade will handle media, comments on those
            // posts, post-hashtags)
            log.info("Deleting posts for user {}", userId);
            postRepository.deleteByUserId(userId);

            // 8. Delete all follow relationships (both as follower and as followed)
            log.info("Deleting follow relationships for user {}", userId);
            followRepository.deleteByFollowerId(userId);
            followRepository.deleteByFollowingId(userId);

            // 9. Delete all notifications (both received and sent)
            log.info("Deleting notifications for user {}", userId);
            notificationRepository.deleteByRecipientId(userId);
            notificationRepository.deleteBySenderId(userId);

            // 10. Delete user profile
            log.info("Deleting user profile for user {}", userId);
            userProfileRepository.deleteByUserId(userId);

            // 11. Delete user settings
            log.info("Deleting user settings for user {}", userId);
            settingsRepository.deleteByUserId(userId);

            // 12. Delete user presence
            log.info("Deleting user presence for user {}", userId);
            if (userPresenceRepository.existsById(userId)) {
                userPresenceRepository.deleteById(userId);
            }

            log.info("All local data deleted for user {}. Now deleting from Keycloak...", userId);

            // 13. Delete from Keycloak — revoking identity so user can never log in again
            boolean keycloakDeleted = keycloakAdminService.deleteUserFromKeycloak(userId);
            if (!keycloakDeleted) {
                log.error("CRITICAL: Local data deleted but Keycloak deletion FAILED for user {}. "
                        + "Manual cleanup required in Keycloak admin console.", userId);
            }
            return keycloakDeleted;
        } catch (Exception e) {
            log.error("Failed to delete account for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Account deletion failed. Please try again or contact support.", e);
        }
    }

    private UserSettings createDefaultSettings(String userId) {
        UserSettings settings = new UserSettings();
        settings.setUserId(userId);
        return settingsRepository.save(settings);
    }

    private UserSettingsDto mapToDto(UserSettings settings) {
        UserSettingsDto dto = new UserSettingsDto();
        BeanUtils.copyProperties(settings, dto);
        return dto;
    }
}
