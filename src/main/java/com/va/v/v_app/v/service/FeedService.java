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
}
