package com.va.v.v_app.social.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model representing a post with its calculated scores
 * Used for ranking and caching in Redis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredPost implements Serializable {

    private static final long serialVersionUID = 1L;

    // Post identification
    private Long postId;
    private String userId;
    private LocalDateTime createdAt;

    // Engagement metrics
    private Integer likesCount;
    private Integer commentsCount;
    private Integer sharesCount;
    private Integer viewsCount;
    private Integer mediaCount;

    // Calculated scores
    private Double engagementScore; // Base engagement without decay
    private Double trendingScore; // With time decay applied
    private Double personalizedScore; // With personalization boost
    private Double engagementVelocity; // Engagement rate over time

    // Score components (for debugging/analysis)
    private Double timeDecayFactor;
    private Double recencyBonus;
    private Double mediaBonus;
    private Double personalizationBoost;

    // Metadata
    private LocalDateTime scoreCalculatedAt;
    private String scoreVersion; // For algorithm versioning

    /**
     * Get the final score used for ranking
     */
    public Double getFinalScore() {
        return personalizedScore != null ? personalizedScore
                : (trendingScore != null ? trendingScore : engagementScore);
    }

    /**
     * Calculate age in hours
     */
    public long getAgeInHours() {
        if (createdAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toHours();
    }

    /**
     * Calculate age in minutes
     */
    public long getAgeInMinutes() {
        if (createdAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }

    /**
     * Check if post is recent (< 24 hours)
     */
    public boolean isRecent() {
        return getAgeInHours() < 24;
    }

    /**
     * Check if post is viral (high velocity)
     */
    public boolean isViral() {
        return engagementVelocity != null && engagementVelocity > 50.0;
    }

    /**
     * Get total engagement count
     */
    public int getTotalEngagement() {
        int likes = likesCount != null ? likesCount : 0;
        int comments = commentsCount != null ? commentsCount : 0;
        int shares = sharesCount != null ? sharesCount : 0;
        return likes + comments + shares;
    }
}
