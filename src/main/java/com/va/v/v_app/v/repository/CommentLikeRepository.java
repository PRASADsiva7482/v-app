package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.CommentLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for CommentLike entity
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, String userId);

    boolean existsByCommentIdAndUserId(Long commentId, String userId);

    void deleteByCommentIdAndUserId(Long commentId, String userId);

    Page<CommentLike> findByCommentId(Long commentId, Pageable pageable);

    long countByCommentId(Long commentId);

    // Account deletion
    void deleteByUserId(String userId);
}
