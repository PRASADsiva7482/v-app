package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.CreatePostRequest;
import com.va.v.v_app.v.dto.request.UpdatePostRequest;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.exception.BusinessException;
import com.va.v.v_app.v.exception.ResourceNotFoundException;
import com.va.v.v_app.v.exception.UnauthorizedException;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.PostLikeRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing posts.
 * 
 * Fixes applied:
 * - B-1: Uses custom exceptions instead of generic RuntimeException
 * - B-6: Cache keys include userId to prevent cross-user cache collision
 * - B-11: View count increment is now async
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserProfileService userProfileService;
    private final MediaService mediaService;
    private final HashtagService hashtagService;

    /**
     * Create a new post
     */
    @Transactional
    public PostResponse createPost(String userId, CreatePostRequest request) {
        // B-1: Use BusinessException instead of RuntimeException
        if ((request.getContent() == null || request.getContent().trim().isEmpty()) &&
                (request.getMediaIds() == null || request.getMediaIds().isEmpty())) {
            throw new BusinessException("EMPTY_POST", "Post must have either content or media");
        }

        Post post = Post.builder()
                .userId(userId)
                .content(request.getContent() != null ? request.getContent() : "")
                .build();

        Post savedPost = postRepository.save(post);

        // Attach media if provided
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            mediaService.attachMediaToPost(savedPost.getId(), request.getMediaIds());
            savedPost.setMediaCount(request.getMediaIds().size());
        }

        // Extract and associate hashtags
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            hashtagService.associateHashtagsWithPost(savedPost, request.getContent());
        }

        userProfileService.incrementPostsCount(userId);

        log.info("Created new post with ID: {} by user: {} with {} media files",
                savedPost.getId(), userId, savedPost.getMediaCount());
        return mapToResponse(savedPost, userId);
    }

    /**
     * Get post by ID
     * 
     * B-6: Cache key now includes currentUserId to prevent cross-user like status
     * collision.
     * Example: User A sees post #123 → caches isLiked=true.
     * User B requests post #123 → should NOT see User A's cached like status.
     */
    @Cacheable(value = "crm:post", key = "#postId + ':' + #currentUserId", unless = "#result == null")
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, String currentUserId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        return mapToResponse(post, currentUserId);
    }

    /**
     * Update post
     * 
     * B-6: Evict ALL user-specific cache entries for this post
     */
    @CacheEvict(value = "crm:post", allEntries = true)
    @Transactional
    public PostResponse updatePost(Long postId, String userId, UpdatePostRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // B-1: Use UnauthorizedException instead of RuntimeException
        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("post", "update");
        }

        post.setContent(request.getContent());
        Post updated = postRepository.save(post);

        // Re-associate hashtags (remove old, add new)
        hashtagService.removeHashtagsFromPost(postId);
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            hashtagService.associateHashtagsWithPost(updated, request.getContent());
        }

        log.info("Updated post ID: {} by user: {}", postId, userId);
        return mapToResponse(updated, userId);
    }

    /**
     * Delete post (soft delete)
     */
    @CacheEvict(value = "crm:post", allEntries = true)
    @Transactional
    public void deletePost(Long postId, String userId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("post", "delete");
        }

        post.setIsDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);

        // Remove hashtag associations
        hashtagService.removeHashtagsFromPost(postId);

        userProfileService.decrementPostsCount(userId);

        log.info("Deleted post ID: {} by user: {}", postId, userId);
    }

    /**
     * Get posts by user
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByUser(String userId, String currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        return posts.map(post -> mapToResponse(post, currentUserId));
    }

    /**
     * Search posts
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> searchPosts(String keyword, String currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.searchPosts(keyword, pageable);
        return posts.map(post -> mapToResponse(post, currentUserId));
    }

    /**
     * Get all posts (explore feed)
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(String currentUserId, Pageable pageable) {
        Page<Post> posts = postRepository.findByIsDeletedFalse(pageable);
        return posts.map(post -> mapToResponse(post, currentUserId));
    }

    /**
     * Increment view count — B-11: Now async for better request throughput.
     * View count is a fire-and-forget operation; the user doesn't need to wait.
     */
    @Async
    @Transactional
    public void incrementViewCount(Long postId) {
        try {
            postRepository.incrementViewCount(postId);
        } catch (Exception e) {
            // Log but don't fail — view count is non-critical
            log.warn("Failed to increment view count for post {}: {}", postId, e.getMessage());
        }
    }

    /**
     * Map entity to response DTO
     */
    public PostResponse mapToResponse(Post post, String currentUserId) {
        UserProfileResponse author = userProfileService.getProfileByUserId(post.getUserId());

        boolean isLiked = currentUserId != null &&
                postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        boolean isOwnPost = currentUserId != null && post.getUserId().equals(currentUserId);

        // Fetch media for the post
        var mediaList = mediaService.getMediaByPostId(post.getId());
        var mediaResponses = mediaService.mapToResponseList(mediaList);

        // Fetch hashtags for the post
        var hashtagResponses = hashtagService.getHashtagsForPost(post.getId());

        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .content(post.getContent())
                .mediaCount(post.getMediaCount())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .repostCount(post.getRepostCount())
                .viewsCount(post.getViewsCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(author)
                .media(mediaResponses)
                .hashtags(hashtagResponses)
                .isLiked(isLiked)
                .isOwnPost(isOwnPost)
                .build();
    }
}
