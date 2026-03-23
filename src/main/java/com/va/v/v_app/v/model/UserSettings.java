package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings", indexes = {
        @Index(name = "idx_user_settings_user", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    // Security & Account Access
    @Column(name = "two_factor_auth", length = 50)
    @Builder.Default
    private String twoFactorAuth = "NONE"; // 'NONE', 'SMS', 'APP', 'SECURITY_KEY'

    @Column(name = "password_protection")
    @Builder.Default
    private Boolean passwordProtection = false;

    @Column(name = "app_sessions_tracking")
    @Builder.Default
    private Boolean appSessionsTracking = true;

    // Privacy & Safety
    @Column(name = "protect_posts")
    @Builder.Default
    private Boolean protectPosts = false;

    @Column(name = "photo_tagging", length = 50)
    @Builder.Default
    private String photoTagging = "ANYONE"; // 'ANYONE', 'FOLLOWING', 'OFF'

    @Column(name = "location_info")
    @Builder.Default
    private Boolean locationInfo = false;

    @Column(name = "sensitive_media")
    @Builder.Default
    private Boolean sensitiveMedia = true;

    @Column(name = "direct_message_privacy", length = 50)
    @Builder.Default
    private String directMessagePrivacy = "EVERYONE"; // 'EVERYONE', 'FOLLOWING', 'NONE'

    @Column(name = "read_receipts")
    @Builder.Default
    private Boolean readReceipts = true;

    @Column(name = "discoverable_by_email")
    @Builder.Default
    private Boolean discoverableByEmail = true;

    @Column(name = "discoverable_by_phone")
    @Builder.Default
    private Boolean discoverableByPhone = true;

    @Column(name = "allow_personalized_ads")
    @Builder.Default
    private Boolean allowPersonalizedAds = true;

    @Column(name = "allow_data_sharing")
    @Builder.Default
    private Boolean allowDataSharing = false;

    // Notifications
    @Column(name = "quality_filter")
    @Builder.Default
    private Boolean qualityFilter = true;

    @Column(name = "mute_accounts_not_following")
    @Builder.Default
    private Boolean muteAccountsNotFollowing = false;

    @Column(name = "mute_accounts_new")
    @Builder.Default
    private Boolean muteAccountsNew = false;

    @Column(name = "push_notifications")
    @Builder.Default
    private Boolean pushNotifications = true;

    @Column(name = "email_notifications")
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(name = "sms_notifications")
    @Builder.Default
    private Boolean smsNotifications = false;

    // Accessibility, display, and languages
    @Column(name = "display_language", length = 50)
    @Builder.Default
    private String displayLanguage = "en";

    @Column(name = "appearance_theme", length = 50)
    @Builder.Default
    private String appearanceTheme = "LIGHT"; // 'LIGHT', 'DIM', 'DARK'

    @Column(name = "font_size", length = 50)
    @Builder.Default
    private String fontSize = "DEFAULT"; // 'SMALL', 'DEFAULT', 'LARGE', 'EXTRA_LARGE'

    @Column(name = "reduce_motion")
    @Builder.Default
    private Boolean reduceMotion = false;

    @Column(name = "data_saver")
    @Builder.Default
    private Boolean dataSaver = false;

    @Column(name = "auto_translate")
    @Builder.Default
    private Boolean autoTranslate = false;

    @Column(name = "translate_language", length = 10)
    @Builder.Default
    private String translateLanguage = "en";

    @Column(name = "chat_encryption")
    @Builder.Default
    private Boolean chatEncryption = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
