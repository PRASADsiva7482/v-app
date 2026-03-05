package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.request.UpdateProfileRequest;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST Controller for user profile operations
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Profile", description = "User profile management APIs")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Get current user's profile")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String userId = authentication.getName();
        UserProfileResponse profile = userProfileService.getProfileByUserId(userId, userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get user profile by userId")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getProfileByUserId(
            @PathVariable String userId,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        UserProfileResponse profile = userProfileService.getProfileByUserId(userId, currentUserId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get user profile by username")
    @GetMapping("/username/{username}")
    public ResponseEntity<UserProfileResponse> getProfileByUsername(
            @PathVariable String username,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        UserProfileResponse profile = userProfileService.getProfileByUsername(username, currentUserId);
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

    @Operation(summary = "Upload and update profile picture (replaces old one)")
    @PostMapping(value = "/me/picture/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        String userId = authentication.getName();
        log.info("Uploading profile picture for user: {}", userId);
        UserProfileResponse updated = userProfileService.uploadAndUpdateProfilePicture(userId, file);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @Operation(summary = "Delete profile picture")
    @DeleteMapping("/me/picture")
    public ResponseEntity<UserProfileResponse> deleteProfilePicture(Authentication authentication) {
        String userId = authentication.getName();
        log.info("Deleting profile picture for user: {}", userId);
        UserProfileResponse updated = userProfileService.deleteProfilePicture(userId);
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

    @Operation(summary = "Search users by username or display name")
    @GetMapping("/search")
    public ResponseEntity<org.springframework.data.domain.Page<UserProfileResponse>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<UserProfileResponse> users = userProfileService.searchUsers(keyword,
                pageable);
        return ResponseEntity.ok(users);
    }
}
