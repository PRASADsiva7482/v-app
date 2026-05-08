package com.va.v.v_app.social.api;

import com.va.v.v_app.social.dto.UserSettingsDto;
import com.va.v.v_app.social.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Managing User Account Settings & Deletion
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settings", description = "User settings, privacy, display, and account deletion APIs")
public class SettingsController {

    private final UserSettingsService settingsService;

    /**
     * Get all settings for the authenticated user
     */
    @GetMapping
    @Operation(summary = "Get user settings", description = "Returns the user's current settings across all categories (Privacy, Security, Display, etc.)")
    public ResponseEntity<UserSettingsDto> getSettings(Authentication authentication) {
        String userId = authentication.getName();
        UserSettingsDto settings = settingsService.getUserSettings(userId);
        return ResponseEntity.ok(settings);
    }

    /**
     * Update settings
     */
    @PutMapping
    @Operation(summary = "Update user settings", description = "Updates specific preferences for the user. Only pass fields that need updating.")
    public ResponseEntity<UserSettingsDto> updateSettings(
            Authentication authentication,
            @RequestBody UserSettingsDto settingsDto) {

        String userId = authentication.getName();
        UserSettingsDto updated = settingsService.updateUserSettings(userId, settingsDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivate / Delete Account Endpoints
     */
    @DeleteMapping("/account")
    @Operation(summary = "Delete user account", description = "Permanently deletes the user's data and removes identity from Keycloak")
    public ResponseEntity<Map<String, Object>> deleteAccount(Authentication authentication) {
        String userId = authentication.getName();
        log.warn("REST request to completely DELETE account for user ID: {}", userId);

        boolean deleted = settingsService.deleteUserAccountEntirely(userId);

        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Account successfully deactivated and deleted from identity provider."));
        } else {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to delete identity from provider. Check system logs."));
        }
    }
}
