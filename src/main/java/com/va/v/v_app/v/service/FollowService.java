package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.FollowStatusResponse;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.model.Follow;
import com.va.v.v_app.v.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing follow relationships between users
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class FollowService {

    private final FollowRepository followRepository;
    private final UserProfileService userProfileService;

    /**
     * Follow a user
     */
    @Transactional
    public void followUser(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("You cannot follow yourself");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("You are already following this user");
        }

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();

        followRepository.save(follow);
        userProfileService.incrementFollowingCount(followerId);
        userProfileService.incrementFollowersCount(followingId);

        log.info("User {} followed user {}", followerId, followingId);
    }

    /**
     * Unfollow a user
     */
    @Transactional
    public void unfollowUser(String followerId, String followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new RuntimeException("You are not following this user");
        }

        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        userProfileService.decrementFollowingCount(followerId);
        userProfileService.decrementFollowersCount(followingId);

        log.info("User {} unfollowed user {}", followerId, followingId);
    }

    /**
     * Get followers of a user
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getFollowers(String userId, Pageable pageable, String currentUserId) {
        Page<Follow> follows = followRepository.findByFollowingId(userId, pageable);
        return follows.map(follow -> userProfileService.getProfileByUserId(follow.getFollowerId(), currentUserId));
    }

    /**
     * Get users that a user is following
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getFollowing(String userId, Pageable pageable, String currentUserId) {
        Page<Follow> follows = followRepository.findByFollowerId(userId, pageable);
        return follows.map(follow -> userProfileService.getProfileByUserId(follow.getFollowingId(), currentUserId));
    }

    /**
     * Check follow status between two users
     */
    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(String currentUserId, String targetUserId) {
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        boolean isFollowedBy = followRepository.existsByFollowerIdAndFollowingId(targetUserId, currentUserId);

        return FollowStatusResponse.builder()
                .userId(targetUserId)
                .isFollowing(isFollowing)
                .isFollowedBy(isFollowedBy)
                .build();
    }

    /**
     * Check if user is following another user
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(String followerId, String followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }
}
