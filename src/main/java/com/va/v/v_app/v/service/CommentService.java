package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.request.CreateCommentRequest;
import com.va.v.v_app.v.dto.response.CommentResponse;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.model.Comment;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.CommentLikeRepository;
import com.va.v.v_app.v.repository.CommentRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing comments
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserProfileService userProfileService;

    /**
     * Add comment to post
     */
    @Transactional
    public CommentResponse addComment(Long postId, String userId, CreateCommentRequest request) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = Comment.builder()
                .post(post)
                .userId(userId)
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);

        log.info("Added comment ID: {} to post: {} by user: {}", saved.getId(), postId, userId);
        return mapToResponse(saved, userId);
    }

    /**
     * Reply to comment
     */
    @Transactional
    public CommentResponse replyToComment(Long commentId, String userId, CreateCommentRequest request) {
        Comment parentComment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        Comment reply = Comment.builder()
                .post(parentComment.getPost())
                .userId(userId)
                .parentComment(parentComment)
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(reply);
        commentRepository.incrementReplyCount(commentId);
        postRepository.incrementCommentCount(parentComment.getPost().getId());

        log.info("Added reply ID: {} to comment: {} by user: {}", saved.getId(), commentId, userId);
        return mapToResponse(saved, userId);
    }

    /**
     * Get comments for post
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsForPost(Long postId, String currentUserId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByPost_IdAndIsDeletedFalseAndParentCommentIsNull(postId,
                pageable);
        return comments.map(comment -> mapToResponse(comment, currentUserId));
    }

    /**
     * Get replies for comment
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getRepliesForComment(Long commentId, String currentUserId) {
        List<Comment> replies = commentRepository.findByParentComment_IdAndIsDeletedFalse(commentId);
        return replies.stream()
                .map(reply -> mapToResponse(reply, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Delete comment (soft delete)
     */
    @Transactional
    public void deleteComment(Long commentId, String userId) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You don't have permission to delete this comment");
        }

        comment.setIsDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);

        postRepository.decrementCommentCount(comment.getPost().getId());
        if (comment.getParentComment() != null) {
            commentRepository.decrementReplyCount(comment.getParentComment().getId());
        }

        log.info("Deleted comment ID: {} by user: {}", commentId, userId);
    }

    /**
     * Map entity to response DTO
     */
    private CommentResponse mapToResponse(Comment comment, String currentUserId) {
        UserProfileResponse author = userProfileService.getProfileByUserId(comment.getUserId());

        boolean isLiked = currentUserId != null &&
                commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);

        boolean isOwnComment = currentUserId != null && comment.getUserId().equals(currentUserId);

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .userId(comment.getUserId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.getContent())
                .likesCount(comment.getLikesCount())
                .repliesCount(comment.getRepliesCount())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .author(author)
                .replies(new ArrayList<>())
                .isLiked(isLiked)
                .isOwnComment(isOwnComment)
                .build();
    }

    // ========== CURSOR-BASED PAGINATION (Infinite Scroll) ==========

    /**
     * Get comments for post with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.v.dto.response.CursorPageResponse<CommentResponse> getCommentsForPostWithCursor(
            Long postId, String currentUserId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Fetch comments with cursor (fetch one extra to determine hasNext)
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit + 1);
        List<Comment> comments = commentRepository.findByPostIdWithCursor(postId, cursor, pageable);

        // Map to response DTOs
        List<CommentResponse> responseList = comments.stream()
                .limit(limit)
                .map(comment -> mapToResponse(comment, currentUserId))
                .toList();

        // Calculate next cursor
        String nextCursor = null;
        if (comments.size() > limit) {
            nextCursor = String.valueOf(responseList.get(responseList.size() - 1).getId());
        }

        return com.va.v.v_app.v.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }

    /**
     * Get replies for comment with cursor-based pagination
     */
    @Transactional(readOnly = true)
    public com.va.v.v_app.v.dto.response.CursorPageResponse<CommentResponse> getRepliesForCommentWithCursor(
            Long commentId, String currentUserId, Long cursor, int limit) {

        // Validate limit
        if (limit > 50)
            limit = 50;
        if (limit < 1)
            limit = 20;

        // Fetch replies with cursor
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit + 1);
        List<Comment> replies = commentRepository.findRepliesWithCursor(commentId, cursor, pageable);

        // Map to response DTOs
        List<CommentResponse> responseList = replies.stream()
                .limit(limit)
                .map(reply -> mapToResponse(reply, currentUserId))
                .toList();

        // Calculate next cursor
        String nextCursor = null;
        if (replies.size() > limit) {
            nextCursor = String.valueOf(responseList.get(responseList.size() - 1).getId());
        }

        return com.va.v.v_app.v.dto.response.CursorPageResponse.of(responseList, nextCursor, limit);
    }
}
