package com.va.v.v_app.v.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDto {
    private String twoFactorAuth;
    private Boolean passwordProtection;
    private Boolean appSessionsTracking;

    private Boolean protectPosts;
    private String photoTagging;
    private Boolean locationInfo;
    private Boolean sensitiveMedia;
    private String directMessagePrivacy;
    private Boolean readReceipts;
    private Boolean discoverableByEmail;
    private Boolean discoverableByPhone;
    private Boolean allowPersonalizedAds;
    private Boolean allowDataSharing;

    private Boolean qualityFilter;
    private Boolean muteAccountsNotFollowing;
    private Boolean muteAccountsNew;
    private Boolean pushNotifications;
    private Boolean emailNotifications;
    private Boolean smsNotifications;

    private String displayLanguage;
    private String appearanceTheme;
    private String fontSize;
    private Boolean reduceMotion;
    private Boolean dataSaver;
}
