package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.FollowStatusResponse;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for follow operations
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Follow", description = "Follow management APIs")
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "Follow a user")
    @PostMapping("/{userId}/follow")
    public ResponseEntity<Void> followUser(
            @PathVariable String userId,
            Authentication authentication) {
        String followerId = authentication.getName();
        followService.followUser(followerId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unfollow a user")
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable String userId,
            Authentication authentication) {
        String followerId = authentication.getName();
        followService.unfollowUser(followerId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get followers of a user")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Page<UserProfileResponse>> getFollowers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> followers = followService.getFollowers(userId, pageable, currentUserId);
        return ResponseEntity.ok(followers);
    }

    @Operation(summary = "Get users that a user is following")
    @GetMapping("/{userId}/following")
    public ResponseEntity<Page<UserProfileResponse>> getFollowing(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> following = followService.getFollowing(userId, pageable, currentUserId);
        return ResponseEntity.ok(following);
    }

    @Operation(summary = "Get follow status between current user and target user")
    @GetMapping("/{userId}/follow-status")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(
            @PathVariable String userId,
            Authentication authentication) {
        String currentUserId = authentication.getName();
        FollowStatusResponse status = followService.getFollowStatus(currentUserId, userId);
        return ResponseEntity.ok(status);
    }
}
