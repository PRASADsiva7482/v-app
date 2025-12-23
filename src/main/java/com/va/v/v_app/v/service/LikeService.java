package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.LikeResponse;
import com.va.v.v_app.v.dto.response.UserProfileResponse;
import com.va.v.v_app.v.model.CommentLike;
import com.va.v.v_app.v.model.PostLike;
import com.va.v.v_app.v.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing likes on posts and comments
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserProfileService userProfileService;

    /**
     * Like a post
     */
    @Transactional
    public void likePost(Long postId, String userId) {
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new RuntimeException("You have already liked this post");
        }

        PostLike like = PostLike.builder()
                .postId(postId)
                .userId(userId)
                .build();

        postLikeRepository.save(like);
        postRepository.incrementLikeCount(postId);

        log.info("User {} liked post {}", userId, postId);
    }

    /**
     * Unlike a post
     */
    @Transactional
    public void unlikePost(Long postId, String userId) {
        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new RuntimeException("You haven't liked this post");
        }

        postLikeRepository.deleteByPostIdAndUserId(postId, userId);
        postRepository.decrementLikeCount(postId);

        log.info("User {} unliked post {}", userId, postId);
    }

    /**
     * Get users who liked a post
     */
    @Transactional(readOnly = true)
    public Page<LikeResponse> getPostLikes(Long postId, Pageable pageable) {
        Page<PostLike> likes = postLikeRepository.findByPostId(postId, pageable);
        return likes.map(this::mapPostLikeToResponse);
    }

    /**
     * Like a comment
     */
    @Transactional
    public void likeComment(Long commentId, String userId) {
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new RuntimeException("You have already liked this comment");
        }

        CommentLike like = CommentLike.builder()
                .commentId(commentId)
                .userId(userId)
                .build();

        commentLikeRepository.save(like);
        commentRepository.incrementLikeCount(commentId);

        log.info("User {} liked comment {}", userId, commentId);
    }

    /**
     * Unlike a comment
     */
    @Transactional
    public void unlikeComment(Long commentId, String userId) {
        if (!commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            throw new RuntimeException("You haven't liked this comment");
        }

        commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
        commentRepository.decrementLikeCount(commentId);

        log.info("User {} unliked comment {}", userId, commentId);
    }

    /**
     * Get users who liked a comment
     */
    @Transactional(readOnly = true)
    public Page<LikeResponse> getCommentLikes(Long commentId, Pageable pageable) {
        Page<CommentLike> likes = commentLikeRepository.findByCommentId(commentId, pageable);
        return likes.map(this::mapCommentLikeToResponse);
    }

    /**
     * Map PostLike to LikeResponse
     */
    private LikeResponse mapPostLikeToResponse(PostLike like) {
        UserProfileResponse user = userProfileService.getProfileByUserId(like.getUserId());
        return LikeResponse.builder()
                .id(like.getId())
                .userId(like.getUserId())
                .createdAt(like.getCreatedAt())
                .user(user)
                .build();
    }

    /**
     * Map CommentLike to LikeResponse
     */
    private LikeResponse mapCommentLikeToResponse(CommentLike like) {
        UserProfileResponse user = userProfileService.getProfileByUserId(like.getUserId());
        return LikeResponse.builder()
                .id(like.getId())
                .userId(like.getUserId())
                .createdAt(like.getCreatedAt())
                .user(user)
                .build();
    }
}
