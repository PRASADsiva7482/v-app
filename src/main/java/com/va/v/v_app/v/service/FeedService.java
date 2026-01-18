package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.FollowRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for generating user feeds
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class FeedService {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final PostService postService;

    /**
     * Get timeline feed (posts from users you follow)
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getTimelineFeed(String userId, Pageable pageable) {
        // Get list of users the current user follows
        List<String> followingIds = followRepository.findFollowingUserIds(userId);

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
    @Transactional(readOnly = true)
    public Page<PostResponse> getExploreFeed(String userId, Pageable pageable) {
        return postService.getAllPosts(userId, pageable);
    }

    /**
     * Get user feed (specific user's posts)
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getUserFeed(String targetUserId, String currentUserId, Pageable pageable) {
        return postService.getPostsByUser(targetUserId, currentUserId, pageable);
    }

    // ========== CURSOR-BASED PAGINATION (Infinite Scroll) ==========

    /**
     * Get timeline feed with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.v.dto.response.CursorPageResponse<PostResponse> getTimelineFeedWithCursor(
            String userId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Get list of users the current user follows
        List<String> followingIds = followRepository.findFollowingUserIds(userId);
        followingIds.add(userId); // Add current user's own posts

        if (followingIds.isEmpty()) {
            // If not following anyone, return empty feed
            return com.va.v.v_app.v.dto.response.CursorPageResponse.of(
                    new java.util.ArrayList<>(), null, limit);
        }

        // Get posts from followed users with cursor
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                limit + 1); // Fetch one extra to determine hasNext
        List<com.va.v.v_app.v.model.Post> posts = postRepository.findByUserIdInWithCursor(followingIds, cursor,
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

        return com.va.v.v_app.v.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }

    /**
     * Get explore feed with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.v.dto.response.CursorPageResponse<PostResponse> getExploreFeedWithCursor(
            String userId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Get all posts with cursor
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                limit + 1); // Fetch one extra
        List<com.va.v.v_app.v.model.Post> posts = postRepository.findWithCursor(cursor, pageable);

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

        return com.va.v.v_app.v.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }

    /**
     * Get user feed with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.v.dto.response.CursorPageResponse<PostResponse> getUserFeedWithCursor(
            String targetUserId, String currentUserId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Get user posts with cursor
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                limit + 1);
        List<com.va.v.v_app.v.model.Post> posts = postRepository.findByUserIdWithCursor(targetUserId, cursor, pageable);

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

        return com.va.v.v_app.v.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }
}
