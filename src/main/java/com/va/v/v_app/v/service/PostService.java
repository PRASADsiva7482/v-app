package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.CreatePostRequest;
import com.va.v.v_app.v.dto.request.UpdatePostRequest;
import com.va.v.v_app.v.dto.response.PollResponse;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.exception.BusinessException;
import com.va.v.v_app.v.exception.ResourceNotFoundException;
import com.va.v.v_app.v.exception.UnauthorizedException;
import com.va.v.v_app.v.model.Comment;
import com.va.v.v_app.v.model.Media;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.CommentLikeRepository;
import com.va.v.v_app.v.repository.CommentRepository;
import com.va.v.v_app.v.repository.MediaRepository;
import com.va.v.v_app.v.repository.PostLikeRepository;
import com.va.v.v_app.v.repository.PostMentionRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing posts.
 *
 * Features:
 * - Create, read, update, delete posts
 * - Time-windowed edit/delete (configurable, default 15 min)
 * - Only the post creator can edit/delete
 * - Hard delete removes all related data (media, comments, likes, hashtags)
 * - Update supports adding/removing media attachments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final MediaRepository mediaRepository;
    private final UserProfileService userProfileService;
    private final MediaService mediaService;
    private final HashtagService hashtagService;
    private final NotificationService notificationService;
    private final PostMentionRepository postMentionRepository;
    private final com.va.v.v_app.v.repository.BookmarkRepository bookmarkRepository;
    private final PollService pollService;

    @Value("${feature.social.post.edit-window-minutes:15}")
    private int editWindowMinutes;

    /**
     * Create a new post
     */
    @Transactional
    public PostResponse createPost(String userId, CreatePostRequest request) {
        if ((request.getContent() == null || request.getContent().trim().isEmpty()) &&
                (request.getMediaIds() == null || request.getMediaIds().isEmpty()) &&
                request.getPoll() == null) {
            throw new BusinessException("EMPTY_POST", "Post must have either content, media, or a poll");
        }

        Post post = Post.builder()
                .userId(userId)
                .content(request.getContent() != null ? request.getContent() : "")
                .isDraft(request.getIsDraft() != null ? request.getIsDraft() : false)
                .scheduledFor(request.getScheduledFor())
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

        // Process Mentions
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            for (String mentionedUserId : request.getMentionedUserIds()) {
                // Ensure duplicate mentions are not stored
                if (!postMentionRepository.findByPostId(savedPost.getId()).stream()
                        .anyMatch(pn -> pn.getMentionedUserId().equals(mentionedUserId))) {
                    com.va.v.v_app.v.model.PostMention postMention = com.va.v.v_app.v.model.PostMention.builder()
                            .post(savedPost)
                            .mentionedUserId(mentionedUserId)
                            .build();

                    postMentionRepository.save(postMention);

                    // Send notification to mentioned user
                    notificationService.notifyMention(mentionedUserId, userId, savedPost.getId());
                }
            }
        }

        // Create poll if provided
        if (request.getPoll() != null) {
            pollService.createPoll(savedPost, request.getPoll());
            log.info("Created poll for post ID: {}", savedPost.getId());
        }

        userProfileService.incrementPostsCount(userId);

        log.info("Created new post with ID: {} by user: {} with {} media files and {} mentions",
                savedPost.getId(), userId, savedPost.getMediaCount(),
                request.getMentionedUserIds() != null ? request.getMentionedUserIds().size() : 0);
        return mapToResponse(savedPost, userId);
    }

    /**
     * Get post by ID
     *
     * B-6: Cache key now includes currentUserId to prevent cross-user like status
     * collision.
     */
    @Cacheable(value = "crm:post", key = "#postId + ':' + #currentUserId", unless = "#result == null")
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, String currentUserId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        return mapToResponse(post, currentUserId);
    }

    /**
     * Update post — only the author can update, and only within the edit window.
     * Supports updating content and adding/removing media attachments.
     */
    @CacheEvict(value = "crm:post", allEntries = true)
    @Transactional
    public PostResponse updatePost(Long postId, String userId, UpdatePostRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Only the author can update
        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("post", "update");
        }

        // Check edit time window
        validateEditWindow(post);

        // Update content if provided
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }

        // Remove media attachments if requested
        if (request.getRemoveMediaIds() != null && !request.getRemoveMediaIds().isEmpty()) {
            for (Long mediaId : request.getRemoveMediaIds()) {
                try {
                    Media media = mediaService.getMediaById(mediaId);
                    // Only remove if this media belongs to this post
                    if (media.getPost() != null && media.getPost().getId().equals(postId)) {
                        mediaService.deleteMedia(mediaId);
                        log.info("Removed media ID: {} from post ID: {}", mediaId, postId);
                    }
                } catch (ResourceNotFoundException e) {
                    log.warn("Media ID: {} not found during post update, skipping", mediaId);
                }
            }
        }

        // Add new media attachments if requested
        if (request.getAddMediaIds() != null && !request.getAddMediaIds().isEmpty()) {
            mediaService.attachMediaToPost(postId, request.getAddMediaIds());
            log.info("Added {} new media to post ID: {}", request.getAddMediaIds().size(), postId);
        }

        // Recalculate media count
        List<Media> currentMedia = mediaService.getMediaByPostId(postId);
        post.setMediaCount(currentMedia.size());

        Post updated = postRepository.save(post);

        // Re-associate hashtags (remove old, add new)
        hashtagService.removeHashtagsFromPost(postId);
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            hashtagService.associateHashtagsWithPost(updated, request.getContent());
        }

        // Re-associate mentions
        postMentionRepository.deleteByPostId(postId);
        if (request.getMentionedUserIds() != null && !request.getMentionedUserIds().isEmpty()) {
            for (String mentionedUserId : request.getMentionedUserIds()) {
                if (!postMentionRepository.findByPostId(postId).stream()
                        .anyMatch(pn -> pn.getMentionedUserId().equals(mentionedUserId))) {
                    com.va.v.v_app.v.model.PostMention postMention = com.va.v.v_app.v.model.PostMention.builder()
                            .post(updated)
                            .mentionedUserId(mentionedUserId)
                            .build();
                    postMentionRepository.save(postMention);
                    // Send notification to mentioned user for new mentions
                    notificationService.notifyMention(mentionedUserId, userId, postId);
                }
            }
        }

        log.info("Updated post ID: {} by user: {}", postId, userId);
        return mapToResponse(updated, userId);
    }

    /**
     * Delete post — HARD DELETE.
     * Only the author can delete, and only within the edit window.
     * Cascades deletion to: media (files + DB), comments, comment likes, post
     * likes, hashtags.
     */
    @CacheEvict(value = "crm:post", allEntries = true)
    @Transactional
    public void deletePost(Long postId, String userId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedException("post", "delete");
        }

        // Check edit time window
        validateEditWindow(post);

        // 1. Delete all media files from storage and DB
        List<Media> mediaList = mediaRepository.findByPostId(postId);
        for (Media media : mediaList) {
            deleteMediaFile(media.getFilePath());
            if (media.getThumbnailUrl() != null) {
                // Try to delete thumbnail file as well
                deleteMediaFile(media.getFilePath().replace(media.getFileName(), "") +
                        "thumbnail_" + media.getFileName());
            }
        }
        mediaRepository.deleteByPost_Id(postId);
        log.info("Deleted {} media files for post ID: {}", mediaList.size(), postId);

        // 2. Delete all comment likes for comments on this post
        List<Comment> comments = commentRepository.findByPost_IdAndIsDeletedFalseAndParentCommentIsNull(
                postId, Pageable.unpaged()).getContent();
        // Collect all comment IDs (top-level + nested replies)
        List<Long> allCommentIds = collectAllCommentIds(comments);
        if (!allCommentIds.isEmpty()) {
            commentLikeRepository.deleteByCommentIdIn(allCommentIds);
            log.info("Deleted comment likes for {} comments on post ID: {}", allCommentIds.size(), postId);
        }

        // 3. Delete all comments for this post
        commentRepository.deleteByPost_Id(postId);
        log.info("Deleted comments for post ID: {}", postId);

        // 4. Delete all post likes
        postLikeRepository.deleteByPostId(postId);
        log.info("Deleted post likes for post ID: {}", postId);

        // 5. Remove hashtag associations
        hashtagService.removeHashtagsFromPost(postId);

        // 6. Hard-delete the post itself
        postRepository.delete(post);

        userProfileService.decrementPostsCount(userId);

        log.info("Hard-deleted post ID: {} and all related data by user: {}", postId, userId);
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
     */
    @Async
    @Transactional
    public void incrementViewCount(Long postId) {
        try {
            postRepository.incrementViewCount(postId);
        } catch (Exception e) {
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

        // Check if post is within the editable time window
        boolean isEditable = isOwnPost && isWithinEditWindow(post);

        // Fetch media for the post
        var mediaList = mediaService.getMediaByPostId(post.getId());
        var mediaResponses = mediaService.mapToResponseList(mediaList);

        // Fetch hashtags for the post
        var hashtagResponses = hashtagService.getHashtagsForPost(post.getId());

        // Fetch mentions for the post
        var mentionedUserIds = postMentionRepository.findByPostId(post.getId()).stream()
                .map(m -> m.getMentionedUserId())
                .collect(Collectors.toList());

        // Check if bookmarked
        boolean isBookmarked = currentUserId != null &&
                bookmarkRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        // Fetch poll data if present
        PollResponse pollResponse = pollService.getPollForPost(post.getId(), currentUserId);

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
                .mentionedUserIds(mentionedUserIds)
                .isLiked(isLiked)
                .isOwnPost(isOwnPost)
                .isEditable(isEditable)
                .isBookmarked(isBookmarked)
                .poll(pollResponse)
                .isDraft(post.getIsDraft())
                .scheduledFor(post.getScheduledFor())
                .build();
    }

    // ==================== Private Helper Methods ====================

    /**
     * Check if the post is within the configurable edit window.
     */
    private boolean isWithinEditWindow(Post post) {
        if (post.getCreatedAt() == null)
            return false;
        LocalDateTime editDeadline = post.getCreatedAt().plusMinutes(editWindowMinutes);
        return LocalDateTime.now().isBefore(editDeadline);
    }

    /**
     * Validate that the post is within the edit window; throw exception if not.
     */
    private void validateEditWindow(Post post) {
        if (!isWithinEditWindow(post)) {
            throw new BusinessException("EDIT_WINDOW_EXPIRED",
                    "Post can only be edited or deleted within " + editWindowMinutes + " minutes of creation");
        }
    }

    /**
     * Recursively collect all comment IDs (including nested replies).
     */
    private List<Long> collectAllCommentIds(List<Comment> comments) {
        List<Long> ids = comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        for (Comment comment : comments) {
            List<Comment> replies = commentRepository.findByParentComment_IdAndIsDeletedFalse(comment.getId());
            if (!replies.isEmpty()) {
                ids.addAll(collectAllCommentIds(replies));
            }
        }
        return ids;
    }

    /**
     * Safely delete a physical media file from storage.
     */
    private void deleteMediaFile(String filePath) {
        if (filePath == null)
            return;
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
            log.debug("Deleted media file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete media file: {}", filePath, e);
        }
    }
}
