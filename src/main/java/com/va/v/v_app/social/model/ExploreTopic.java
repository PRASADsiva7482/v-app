package com.va.v.v_app.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a trending topic on the Explore page
 */
@Entity
@Table(name = "explore_topic", indexes = {
        @Index(name = "idx_explore_topic_category", columnList = "category_id"),
        @Index(name = "idx_explore_topic_trending", columnList = "trending_score"),
        @Index(name = "idx_explore_topic_active", columnList = "is_active, trending_score"),
        @Index(name = "idx_explore_topic_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExploreCategory category;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "post_count")
    @Builder.Default
    private Long postCount = 0L;

    @Column(name = "is_hashtag")
    @Builder.Default
    private Boolean isHashtag = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_promoted")
    @Builder.Default
    private Boolean isPromoted = false;

    @Column(name = "trending_score")
    @Builder.Default
    private Double trendingScore = 0.0;

    @Column(name = "started_trending_at")
    private LocalDateTime startedTrendingAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
