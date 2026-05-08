package com.va.v.v_app.social.service;

import com.va.v.v_app.social.dto.response.PostResponse;
import com.va.v.v_app.social.model.Post;
import com.va.v.v_app.social.model.UserProfile;
import com.va.v.v_app.social.repository.FollowRepository;
import com.va.v.v_app.social.repository.PostRepository;
import com.va.v.v_app.social.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for generating user feeds
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final PostService postService;
    private final UserProfileRepository userProfileRepository;

    /**
     * Get timeline feed (posts from users you follow)
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getTimelineFeed(String userId, Pageable pageable) {
        // Get list of users the current user ACTUALLY follows (ACCEPTED only)
        List<String> followingIds = followRepository.findAcceptedFollowingUserIds(userId);

        // Add current user's own posts to the feed
        followingIds.add(userId);

        if (followingIds.isEmpty()) {
            // If not following anyone, return empty feed
            return Page.empty(pageable);
        }

        // Get posts from followed users
        Page<Post> posts = postRepository.findByUserIdIn(followingIds, pageable);

        // Map to response DTOs
        List<PostResponse> responseList = posts.getContent().stream()
                .map(post -> postService.getPostById(post.getId(), userId))
                .toList();

        return new PageImpl<>(responseList, pageable, posts.getTotalElements());
    }

    /**
     * Get explore feed (all public posts)
     */
    /**
     * Get explore feed (all public posts).
     * Filters out posts from private users unless the current user follows them.
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getExploreFeed(String userId, Pageable pageable) {
        Page<PostResponse> allPosts = postService.getAllPosts(userId, pageable);

        // Get the set of private user IDs and the set of users the current user follows
        Set<String> followingIds = userId != null
                ? Set.copyOf(followRepository.findAcceptedFollowingUserIds(userId))
                : Set.of();

        // Filter out posts from private users that current user doesn't follow
        List<PostResponse> filtered = allPosts.getContent().stream()
                .filter(post -> {
                    if (userId != null && userId.equals(post.getUserId())) return true; // Own posts always visible
                    UserProfile author = userProfileRepository.findByUserId(post.getUserId()).orElse(null);
                    if (author == null) return true;
                    if (!Boolean.TRUE.equals(author.getIsPrivate())) return true; // Public posts visible
                    return followingIds.contains(post.getUserId()); // Private only if following
                })
                .toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    /**
     * Get user feed (specific user's posts)
     */
    /**
     * Get user feed (specific user's posts).
     * Returns empty page if the target user is private and the current user doesn't follow them.
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getUserFeed(String targetUserId, String currentUserId, Pageable pageable) {
        // Check if target is private and current user isn't following
        if (currentUserId != null && !currentUserId.equals(targetUserId)) {
            UserProfile targetProfile = userProfileRepository.findByUserId(targetUserId).orElse(null);
            if (targetProfile != null && Boolean.TRUE.equals(targetProfile.getIsPrivate())) {
                boolean isFollowing = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                        currentUserId, targetUserId, 
                        com.va.v.v_app.social.model.Follow.FollowStatus.ACCEPTED);
                if (!isFollowing) {
                    return Page.empty(pageable); // Private user, not following → empty feed
                }
            }
        }
        return postService.getPostsByUser(targetUserId, currentUserId, pageable);
    }

    // ========== CURSOR-BASED PAGINATION (Infinite Scroll) ==========

    /**
     * Get timeline feed with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.social.dto.response.CursorPageResponse<PostResponse> getTimelineFeedWithCursor(
            String userId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Get list of users the current user ACTUALLY follows (ACCEPTED only)
        List<String> followingIds = followRepository.findAcceptedFollowingUserIds(userId);
        followingIds.add(userId); // Add current user's own posts

        if (followingIds.isEmpty()) {
            // If not following anyone, return empty feed
            return com.va.v.v_app.social.dto.response.CursorPageResponse.of(
                    new java.util.ArrayList<>(), null, limit);
        }

        // Get posts from followed users with cursor
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                limit + 1); // Fetch one extra to determine hasNext
        List<com.va.v.v_app.social.model.Post> posts = postRepository.findByUserIdInWithCursor(followingIds, cursor,
                pageable);

        // Map to response DTOs
        List<PostResponse> responseList = posts.stream()
                .limit(limit)
                .map(post -> postService.mapToResponse(post, userId))
                .toList();

        // Calculate next cursor
        String nextCursor = null;
        if (posts.size() > limit) {
            // There are more posts
            nextCursor = String.valueOf(responseList.get(responseList.size() - 1).getId());
        }

        return com.va.v.v_app.social.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }

    /**
     * Get explore feed with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.social.dto.response.CursorPageResponse<PostResponse> getExploreFeedWithCursor(
            String userId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Get all posts with cursor
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                limit + 1); // Fetch one extra
        List<com.va.v.v_app.social.model.Post> posts = postRepository.findWithCursor(cursor, pageable);

        // Map to response DTOs
        List<PostResponse> responseList = posts.stream()
                .limit(limit)
                .map(post -> postService.mapToResponse(post, userId))
                .toList();

        // Calculate next cursor
        String nextCursor = null;
        if (posts.size() > limit) {
            nextCursor = String.valueOf(responseList.get(responseList.size() - 1).getId());
        }

        return com.va.v.v_app.social.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }

    /**
     * Get user feed with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.social.dto.response.CursorPageResponse<PostResponse> getUserFeedWithCursor(
            String targetUserId, String currentUserId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Get user posts with cursor
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                limit + 1);
        List<com.va.v.v_app.social.model.Post> posts = postRepository.findByUserIdWithCursor(targetUserId, cursor, pageable);

        // Map to response DTOs
        List<PostResponse> responseList = posts.stream()
                .limit(limit)
                .map(post -> postService.mapToResponse(post, currentUserId))
                .toList();

        // Calculate next cursor
        String nextCursor = null;
        if (posts.size() > limit) {
            nextCursor = String.valueOf(responseList.get(responseList.size() - 1).getId());
        }

        return com.va.v.v_app.social.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }
}
