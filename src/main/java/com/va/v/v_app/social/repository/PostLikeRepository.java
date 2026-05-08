package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PostLike entity
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserId(Long postId, String userId);

    boolean existsByPostIdAndUserId(Long postId, String userId);

    void deleteByPostIdAndUserId(Long postId, String userId);

    Page<PostLike> findByPostId(Long postId, Pageable pageable);

    long countByPostId(Long postId);

    // Batch loading for multiple posts (prevents N+1 queries)
    @Query("SELECT pl FROM PostLike pl WHERE pl.postId IN :postIds AND pl.userId = :userId")
    List<PostLike> findByPostIdInAndUserId(@Param("postIds") List<Long> postIds, @Param("userId") String userId);

    // Account deletion
    void deleteByUserId(String userId);

    // Post deletion - remove all likes for a post
    void deleteByPostId(Long postId);

    // Analytics: count total likes received on all posts by a user
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.postId IN (SELECT p.id FROM Post p WHERE p.userId = :userId AND p.isDeleted = false)")
    long countLikesByPostOwner(@Param("userId") String userId);
}
