package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Poll entity
 */
@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {

    @Query("SELECT p FROM Poll p LEFT JOIN FETCH p.options WHERE p.post.id = :postId")
    Optional<Poll> findByPostIdWithOptions(@Param("postId") Long postId);

    Optional<Poll> findByPostId(Long postId);

    boolean existsByPostId(Long postId);

    // Find expired polls that haven't been closed yet
    @Query("SELECT p FROM Poll p WHERE p.isClosed = false AND p.expiresAt <= :now")
    List<Poll> findExpiredOpenPolls(@Param("now") LocalDateTime now);

    // Batch loading for feed - find polls for multiple posts
    @Query("SELECT p FROM Poll p LEFT JOIN FETCH p.options WHERE p.post.id IN :postIds")
    List<Poll> findByPostIdInWithOptions(@Param("postIds") List<Long> postIds);

    void deleteByPostId(Long postId);
}
