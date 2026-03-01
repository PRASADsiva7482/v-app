package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.UserSettingsDto;
import com.va.v.v_app.v.model.UserSettings;
import com.va.v.v_app.v.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserSettingsService {

    private final UserSettingsRepository settingsRepository;
    private final KeycloakAdminService keycloakAdminService;

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
     * High-level Account Deletion Flow
     * In a distributed microservice, this would publish a 'UserDeletedEvent'.
     * Since this is monolithic-ish, we delete settings and call Keycloak.
     */
    @Transactional
    public boolean deleteUserAccountEntirely(String userId) {
        log.warn("Executing FULL deletion of account for user {}", userId);

        // 1. Delete Settings
        settingsRepository.deleteByUserId(userId);

        // 2. Here you could add more database cleanup
        // For example: postService.deleteAllUserPosts(userId);
        // commentService.deleteAllUserComments(userId);
        // profileService.deleteProfile(userId);

        // 3. Delete from Keycloak - Revoking Identity
        // Returns true if Keycloak deleted the identity (or if we simulated success)
        return keycloakAdminService.deleteUserFromKeycloak(userId);
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
