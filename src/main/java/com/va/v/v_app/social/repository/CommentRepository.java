package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Comment entity
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

        Optional<Comment> findByIdAndIsDeletedFalse(Long id);

        Page<Comment> findByPost_IdAndIsDeletedFalseAndParentCommentIsNull(Long postId, Pageable pageable);

        List<Comment> findByParentComment_IdAndIsDeletedFalse(Long parentCommentId);

        long countByPost_IdAndIsDeletedFalse(Long postId);

        @Modifying
        @Query("UPDATE Comment c SET c.likesCount = c.likesCount + 1 WHERE c.id = :commentId")
        void incrementLikeCount(@Param("commentId") Long commentId);

        @Modifying
        @Query("UPDATE Comment c SET c.likesCount = c.likesCount - 1 WHERE c.id = :commentId AND c.likesCount > 0")
        void decrementLikeCount(@Param("commentId") Long commentId);

        @Modifying
        @Query("UPDATE Comment c SET c.repliesCount = c.repliesCount + 1 WHERE c.id = :commentId")
        void incrementReplyCount(@Param("commentId") Long commentId);

        @Modifying
        @Query("UPDATE Comment c SET c.repliesCount = c.repliesCount - 1 WHERE c.id = :commentId AND c.repliesCount > 0")
        void decrementReplyCount(@Param("commentId") Long commentId);

        // ========== CURSOR-BASED PAGINATION (Infinite Scroll) ==========

        /**
         * Get comments for a post with cursor-based pagination
         */
        @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.isDeleted = false " +
                        "AND c.parentComment IS NULL " +
                        "AND (:cursor IS NULL OR c.id < :cursor) " +
                        "ORDER BY c.id DESC")
        List<Comment> findByPostIdWithCursor(@Param("postId") Long postId, @Param("cursor") Long cursor,
                        Pageable pageable);

        /**
         * Get replies for a comment with cursor-based pagination
         */
        @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :parentCommentId AND c.isDeleted = false " +
                        "AND (:cursor IS NULL OR c.id < :cursor) " +
                        "ORDER BY c.id DESC")
        List<Comment> findRepliesWithCursor(@Param("parentCommentId") Long parentCommentId,
                        @Param("cursor") Long cursor,
                        Pageable pageable);

        // Account deletion
        void deleteByUserId(String userId);

        // Post deletion - remove all comments for a post
        void deleteByPost_Id(Long postId);
}
