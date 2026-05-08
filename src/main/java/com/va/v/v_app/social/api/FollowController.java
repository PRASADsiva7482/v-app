package com.va.v.v_app.social.api;

import com.va.v.v_app.social.dto.response.FollowStatusResponse;
import com.va.v.v_app.social.dto.response.UserProfileResponse;
import com.va.v.v_app.social.model.Follow.FollowStatus;
import com.va.v.v_app.social.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for follow operations.
 * Supports follow requests for private profiles (Instagram-style).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Follow", description = "Follow management APIs with private profile support")
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "Follow a user (or send follow request if private)")
    @PostMapping("/{userId}/follow")
    public ResponseEntity<Map<String, Object>> followUser(
            @PathVariable String userId,
            Authentication authentication) {
        String followerId = authentication.getName();
        FollowStatus status = followService.followUser(followerId, userId);

        return ResponseEntity.ok(Map.of(
                "status", status.name(),
                "message", status == FollowStatus.ACCEPTED
                        ? "Successfully followed user"
                        : "Follow request sent"
        ));
    }

    @Operation(summary = "Unfollow a user or cancel follow request")
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable String userId,
            Authentication authentication) {
        String followerId = authentication.getName();
        followService.unfollowUser(followerId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Accept a follow request (for private accounts)")
    @PutMapping("/{userId}/follow/accept")
    public ResponseEntity<Map<String, String>> acceptFollowRequest(
            @PathVariable String userId,
            Authentication authentication) {
        String profileOwnerId = authentication.getName();
        followService.acceptFollowRequest(profileOwnerId, userId);
        return ResponseEntity.ok(Map.of("message", "Follow request accepted"));
    }

    @Operation(summary = "Decline a follow request (for private accounts)")
    @PutMapping("/{userId}/follow/decline")
    public ResponseEntity<Map<String, String>> declineFollowRequest(
            @PathVariable String userId,
            Authentication authentication) {
        String profileOwnerId = authentication.getName();
        followService.declineFollowRequest(profileOwnerId, userId);
        return ResponseEntity.ok(Map.of("message", "Follow request declined"));
    }

    @Operation(summary = "Get pending follow requests for current user")
    @GetMapping("/follow-requests")
    public ResponseEntity<Page<UserProfileResponse>> getPendingFollowRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String userId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfileResponse> requests = followService.getPendingFollowRequests(userId, pageable);
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Get count of pending follow requests")
    @GetMapping("/follow-requests/count")
    public ResponseEntity<Map<String, Long>> getPendingFollowRequestsCount(
            Authentication authentication) {
        String userId = authentication.getName();
        long count = followService.getPendingFollowRequestsCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
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
