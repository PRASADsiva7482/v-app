package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.CreatePostRequest;
import com.va.v.v_app.v.dto.request.UpdatePostRequest;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.PostLikeRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Service for managing posts
 */
@Service
@RequiredArgsConstructor
@Log4j2
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
        // Validate that either content or media is provided
        if ((request.getContent() == null || request.getContent().trim().isEmpty()) &&
                (request.getMediaIds() == null || request.getMediaIds().isEmpty())) {
            throw new RuntimeException("Post must have either content or media");
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
     * Cached for 5 minutes to improve performance for frequently viewed posts
     */
    @Cacheable(value = "crm:post", key = "#postId", unless = "#result == null")
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, String currentUserId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
        return mapToResponse(post, currentUserId);
    }

    /**
     * Update post
     * 
     * Evicts post cache to ensure fresh data is fetched on next read
     */
    @CacheEvict(value = "crm:post", key = "#postId")
    @Transactional
    public PostResponse updatePost(Long postId, String userId, UpdatePostRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to update this post");
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
     * 
     * Evicts post cache since the post is being deleted
     */
    @CacheEvict(value = "crm:post", key = "#postId")
    @Transactional
    public void deletePost(Long postId, String userId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this post");
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
     * Increment view count
     * 
     * Evicts cache to reflect updated view count
     * Note: For high-traffic posts, consider async cache update or delayed eviction
     */
    @CacheEvict(value = "crm:post", key = "#postId")
    @Transactional
    public void incrementViewCount(Long postId) {
        postRepository.incrementViewCount(postId);
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
