package com.va.v.v_app.social.service;

import com.va.v.v_app.social.dto.response.FollowStatusResponse;
import com.va.v.v_app.social.dto.response.UserProfileResponse;
import com.va.v.v_app.social.model.Follow;
import com.va.v.v_app.social.model.Follow.FollowStatus;
import com.va.v.v_app.social.model.UserProfile;
import com.va.v.v_app.social.repository.FollowRepository;
import com.va.v.v_app.social.repository.UserProfileRepository;
import com.va.v.v_app.social.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing follow relationships between users.
 * Supports follow requests for private profiles (Instagram-style).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final FollowRepository followRepository;
    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;
    private final NotificationService notificationService;

    /**
     * Follow a user.
     * - If target is PUBLIC: instant follow (ACCEPTED)
     * - If target is PRIVATE: creates a follow request (PENDING)
     * Returns the status of the follow after the operation.
     */
    @Transactional
    public FollowStatus followUser(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException("SELF_FOLLOW", "You cannot follow yourself");
        }

        // Check if a follow/request already exists
        var existingFollow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        if (existingFollow.isPresent()) {
            Follow existing = existingFollow.get();
            if (existing.getStatus() == FollowStatus.ACCEPTED) {
                throw new BusinessException("ALREADY_FOLLOWING", "You are already following this user");
            }
            if (existing.getStatus() == FollowStatus.PENDING) {
                throw new BusinessException("REQUEST_PENDING", "Follow request already sent");
            }
            // If DECLINED, allow re-requesting: delete old and create new
            followRepository.delete(existing);
        }

        // Check if target user is private
        UserProfile targetProfile = userProfileRepository.findByUserId(followingId).orElse(null);
        boolean isTargetPrivate = targetProfile != null && Boolean.TRUE.equals(targetProfile.getIsPrivate());

        FollowStatus status = isTargetPrivate ? FollowStatus.PENDING : FollowStatus.ACCEPTED;

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .status(status)
                .build();

        followRepository.save(follow);

        if (status == FollowStatus.ACCEPTED) {
            // Instant follow — update counts and notify
            userProfileService.incrementFollowingCount(followerId);
            userProfileService.incrementFollowersCount(followingId);
            notificationService.notifyFollow(followingId, followerId);
            log.info("User {} followed user {} (instant, public profile)", followerId, followingId);
        } else {
            // Follow request — notify but don't update counts
            notificationService.notifyFollowRequest(followingId, followerId);
            log.info("User {} sent follow request to private user {}", followerId, followingId);
        }

        return status;
    }

    /**
     * Unfollow a user (also cancels pending requests).
     */
    @Transactional
    public void unfollowUser(String followerId, String followingId) {
        var existingFollow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        if (existingFollow.isEmpty()) {
            throw new BusinessException("NOT_FOLLOWING", "You are not following this user");
        }

        Follow follow = existingFollow.get();
        boolean wasAccepted = follow.getStatus() == FollowStatus.ACCEPTED;

        followRepository.delete(follow);

        if (wasAccepted) {
            // Only decrement counts if the follow was accepted
            userProfileService.decrementFollowingCount(followerId);
            userProfileService.decrementFollowersCount(followingId);
        }

        log.info("User {} unfollowed/cancelled request to user {} (was {})", followerId, followingId, follow.getStatus());
    }

    /**
     * Accept a follow request (only for private accounts).
     */
    @Transactional
    public void acceptFollowRequest(String profileOwnerId, String requesterId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(requesterId, profileOwnerId)
                .orElseThrow(() -> new BusinessException("NO_REQUEST", "No pending follow request found"));

        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new BusinessException("NOT_PENDING", "This follow request is not pending");
        }

        follow.setStatus(FollowStatus.ACCEPTED);
        followRepository.save(follow);

        // Now update counts since the follow is accepted
        userProfileService.incrementFollowingCount(requesterId);
        userProfileService.incrementFollowersCount(profileOwnerId);

        // Notify the requester that their request was accepted
        notificationService.notifyFollowAccept(requesterId, profileOwnerId);

        log.info("User {} accepted follow request from {}", profileOwnerId, requesterId);
    }

    /**
     * Decline a follow request.
     */
    @Transactional
    public void declineFollowRequest(String profileOwnerId, String requesterId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(requesterId, profileOwnerId)
                .orElseThrow(() -> new BusinessException("NO_REQUEST", "No pending follow request found"));

        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new BusinessException("NOT_PENDING", "This follow request is not pending");
        }

        // Delete the follow record entirely
        followRepository.delete(follow);

        log.info("User {} declined follow request from {}", profileOwnerId, requesterId);
    }

    /**
     * Get pending follow requests for the current user (as profile owner).
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getPendingFollowRequests(String userId, Pageable pageable) {
        Page<Follow> pendingFollows = followRepository.findByFollowingIdAndStatusOrderByCreatedAtDesc(
                userId, FollowStatus.PENDING, pageable);
        return pendingFollows.map(follow -> userProfileService.getProfileByUserId(follow.getFollowerId(), userId));
    }

    /**
     * Get count of pending follow requests for a user.
     */
    @Transactional(readOnly = true)
    public long getPendingFollowRequestsCount(String userId) {
        return followRepository.countByFollowingIdAndStatus(userId, FollowStatus.PENDING);
    }

    /**
     * Get followers of a user (ACCEPTED only).
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getFollowers(String userId, Pageable pageable, String currentUserId) {
        Page<Follow> follows = followRepository.findByFollowingIdAndStatus(userId, FollowStatus.ACCEPTED, pageable);
        return follows.map(follow -> userProfileService.getProfileByUserId(follow.getFollowerId(), currentUserId));
    }

    /**
     * Get users that a user is following (ACCEPTED only).
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getFollowing(String userId, Pageable pageable, String currentUserId) {
        Page<Follow> follows = followRepository.findByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED, pageable);
        return follows.map(follow -> userProfileService.getProfileByUserId(follow.getFollowingId(), currentUserId));
    }

    /**
     * Check follow status between two users (status-aware).
     */
    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(String currentUserId, String targetUserId) {
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                currentUserId, targetUserId, FollowStatus.ACCEPTED);
        boolean isFollowedBy = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                targetUserId, currentUserId, FollowStatus.ACCEPTED);
        boolean isPending = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                currentUserId, targetUserId, FollowStatus.PENDING);

        // Check if target is private
        UserProfile targetProfile = userProfileRepository.findByUserId(targetUserId).orElse(null);
        boolean isTargetPrivate = targetProfile != null && Boolean.TRUE.equals(targetProfile.getIsPrivate());

        return FollowStatusResponse.builder()
                .userId(targetUserId)
                .isFollowing(isFollowing)
                .isFollowedBy(isFollowedBy)
                .isFollowRequestPending(isPending)
                .isTargetPrivate(isTargetPrivate)
                .build();
    }

    /**
     * Check if user is actively following another user (ACCEPTED status only).
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(String followerId, String followingId) {
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                followerId, followingId, FollowStatus.ACCEPTED);
    }

    /**
     * Check if there is a pending follow request.
     */
    @Transactional(readOnly = true)
    public boolean isFollowRequestPending(String followerId, String followingId) {
        return followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                followerId, followingId, FollowStatus.PENDING);
    }
}
