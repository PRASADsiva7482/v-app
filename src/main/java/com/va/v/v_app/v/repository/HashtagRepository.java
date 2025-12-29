package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.Hashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Hashtag entity
 */
@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    Optional<Hashtag> findByTagName(String tagName);

    List<Hashtag> findByTagNameIn(List<String> tagNames);

    // Search hashtags by partial name match
    @Query("SELECT h FROM Hashtag h WHERE LOWER(h.tagName) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY h.usageCount DESC")
    Page<Hashtag> searchHashtagsByPrefix(@Param("prefix") String prefix, Pageable pageable);

    // Get trending hashtags (most used in recent time)
    @Query("SELECT h FROM Hashtag h WHERE h.lastUsedAt >= :since ORDER BY h.usageCount DESC")
    Page<Hashtag> findTrendingHashtags(@Param("since") LocalDateTime since, Pageable pageable);

    // Get top hashtags by usage count
    Page<Hashtag> findAllByOrderByUsageCountDesc(Pageable pageable);

    // Increment usage count
    @Modifying
    @Query("UPDATE Hashtag h SET h.usageCount = h.usageCount + 1, h.lastUsedAt = :timestamp WHERE h.id = :hashtagId")
    void incrementUsageCount(@Param("hashtagId") Long hashtagId, @Param("timestamp") LocalDateTime timestamp);

    // Decrement usage count
    @Modifying
    @Query("UPDATE Hashtag h SET h.usageCount = h.usageCount - 1 WHERE h.id = :hashtagId AND h.usageCount > 0")
    void decrementUsageCount(@Param("hashtagId") Long hashtagId);
}
