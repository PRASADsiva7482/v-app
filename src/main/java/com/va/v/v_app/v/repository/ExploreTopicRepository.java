package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.ExploreTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ExploreTopic entity
 */
@Repository
public interface ExploreTopicRepository extends JpaRepository<ExploreTopic, Long> {

    // Get active topics ordered by trending score
    @Query("SELECT t FROM ExploreTopic t LEFT JOIN FETCH t.category WHERE t.isActive = true " +
            "AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY t.trendingScore DESC")
    List<ExploreTopic> findActiveTrendingTopics(Pageable pageable);

    // Get active topics by category
    @Query("SELECT t FROM ExploreTopic t LEFT JOIN FETCH t.category WHERE t.isActive = true " +
            "AND t.category.name = :categoryName " +
            "AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY t.trendingScore DESC")
    List<ExploreTopic> findActiveTrendingTopicsByCategory(
            @Param("categoryName") String categoryName, Pageable pageable);

    // Get active topics (paginated)
    @Query("SELECT t FROM ExploreTopic t LEFT JOIN FETCH t.category WHERE t.isActive = true " +
            "AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY t.trendingScore DESC")
    Page<ExploreTopic> findActiveTopicsPaged(Pageable pageable);

    // Search topics by title
    @Query("SELECT t FROM ExploreTopic t LEFT JOIN FETCH t.category WHERE t.isActive = true " +
            "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY t.trendingScore DESC")
    List<ExploreTopic> searchTopics(@Param("query") String query, Pageable pageable);

    // Count active topics
    long countByIsActiveTrue();
}
