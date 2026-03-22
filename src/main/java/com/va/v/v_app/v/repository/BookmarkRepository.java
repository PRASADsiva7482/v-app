package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Bookmark entity
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByPostIdAndUserId(Long postId, String userId);

    boolean existsByPostIdAndUserId(Long postId, String userId);

    void deleteByPostIdAndUserId(Long postId, String userId);

    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserId(String userId);

    // Batch loading for multiple posts (prevents N+1 queries)
    @Query("SELECT b FROM Bookmark b WHERE b.postId IN :postIds AND b.userId = :userId")
    List<Bookmark> findByPostIdInAndUserId(@Param("postIds") List<Long> postIds, @Param("userId") String userId);

    // Account deletion
    void deleteByUserId(String userId);

    // Post deletion - remove all bookmarks for a post
    void deleteByPostId(Long postId);
}
