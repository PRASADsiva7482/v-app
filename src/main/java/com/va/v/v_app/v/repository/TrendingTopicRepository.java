package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.TrendingTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrendingTopicRepository extends JpaRepository<TrendingTopic, Long> {

    /**
     * Find active trending topics by category
     */
    Page<TrendingTopic> findByIsActiveTrueAndCategoryOrderByTrendScoreDesc(
            String category, Pageable pageable);

    /**
     * Find all active trending topics across all categories
     */
    Page<TrendingTopic> findByIsActiveTrueOrderByTrendScoreDesc(Pageable pageable);

    /**
     * Find trending topics by region and category
     */
    Page<TrendingTopic> findByIsActiveTrueAndCategoryAndRegionOrderByTrendScoreDesc(
            String category, String region, Pageable pageable);

    /**
     * Find trending topics created after a specific date
     */
    @Query("SELECT tt FROM TrendingTopic tt WHERE tt.isActive = true AND tt.createdAt > :since ORDER BY tt.trendScore DESC")
    List<TrendingTopic> findRecentTrending(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Find trending topic by hashtag
     */
    TrendingTopic findByHashtagIdAndIsActiveTrue(Long hashtagId);

    /**
     * Count active trending topics by category
     */
    long countByIsActiveTrueAndCategory(String category);
}
