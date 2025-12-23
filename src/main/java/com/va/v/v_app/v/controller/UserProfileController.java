package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.request.UpdateProfileRequest;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user profile operations
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "User Profile", description = "User profile management APIs")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Get current user's profile")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String userId = authentication.getName();
        UserProfileResponse profile = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get user profile by userId")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(@PathVariable String userId) {
        UserProfileResponse profile = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get user profile by username")
    @GetMapping("/username/{username}")
    public ResponseEntity<UserProfileResponse> getProfileByUsername(@PathVariable String username) {
        UserProfileResponse profile = userProfileService.getProfileByUsername(username);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update current user's profile")
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String userId = authentication.getName();
        UserProfileResponse updated = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Update profile picture URL")
    @PutMapping("/me/picture")
    public ResponseEntity<UserProfileResponse> updateProfilePicture(
            Authentication authentication,
            @RequestParam String pictureUrl) {
        String userId = authentication.getName();
        UserProfileResponse updated = userProfileService.updateProfilePicture(userId, pictureUrl);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Update cover photo URL")
    @PutMapping("/me/cover")
    public ResponseEntity<UserProfileResponse> updateCoverPhoto(
            Authentication authentication,
            @RequestParam String coverPhotoUrl) {
        String userId = authentication.getName();
        UserProfileResponse updated = userProfileService.updateCoverPhoto(userId, coverPhotoUrl);
        return ResponseEntity.ok(updated);
    }
}
