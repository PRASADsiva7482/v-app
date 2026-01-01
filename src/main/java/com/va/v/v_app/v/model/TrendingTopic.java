package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a trending topic
 */
@Entity
@Table(name = "trending_topic", indexes = {
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_trend_score", columnList = "trend_score DESC"),
        @Index(name = "idx_created_at", columnList = "created_at DESC"),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // FOR_YOU, TRENDING, NEWS, SPORTS, ENTERTAINMENT

    @Column(name = "hashtag_id")
    private Long hashtagId; // Reference to hashtag if topic is based on hashtag

    @Column(name = "post_count", nullable = false)
    @Builder.Default
    private Long postCount = 0L;

    @Column(name = "trend_score", nullable = false)
    @Builder.Default
    private Double trendScore = 0.0;

    @Column(name = "region", length = 100)
    private String region; // e.g., "Global", "India", "US"

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "trending_since")
    private LocalDateTime trendingSince;
}
