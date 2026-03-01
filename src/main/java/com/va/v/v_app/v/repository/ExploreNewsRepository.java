package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.ExploreNews;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ExploreNews entity
 */
@Repository
public interface ExploreNewsRepository extends JpaRepository<ExploreNews, Long> {

    // Get active news ordered by published date
    @Query("SELECT n FROM ExploreNews n LEFT JOIN FETCH n.category WHERE n.isActive = true " +
            "AND (n.expiresAt IS NULL OR n.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY n.isBreaking DESC, n.publishedAt DESC")
    List<ExploreNews> findActiveNews(Pageable pageable);

    // Get active news by category
    @Query("SELECT n FROM ExploreNews n LEFT JOIN FETCH n.category WHERE n.isActive = true " +
            "AND n.category.name = :categoryName " +
            "AND (n.expiresAt IS NULL OR n.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY n.isBreaking DESC, n.publishedAt DESC")
    List<ExploreNews> findActiveNewsByCategory(
            @Param("categoryName") String categoryName, Pageable pageable);

    // Get breaking news
    @Query("SELECT n FROM ExploreNews n LEFT JOIN FETCH n.category WHERE n.isActive = true " +
            "AND n.isBreaking = true " +
            "AND (n.expiresAt IS NULL OR n.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY n.publishedAt DESC")
    List<ExploreNews> findBreakingNews(Pageable pageable);

    // Count active news
    long countByIsActiveTrue();
}
