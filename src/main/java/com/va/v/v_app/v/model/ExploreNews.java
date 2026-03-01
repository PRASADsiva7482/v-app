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
 * Entity representing a news item on the Explore page
 */
@Entity
@Table(name = "explore_news", indexes = {
        @Index(name = "idx_explore_news_category", columnList = "category_id"),
        @Index(name = "idx_explore_news_active", columnList = "is_active, published_at"),
        @Index(name = "idx_explore_news_breaking", columnList = "is_breaking, is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "headline", nullable = false, length = 500)
    private String headline;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "source", length = 255)
    private String source;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExploreCategory category;

    @Column(name = "post_count")
    @Builder.Default
    private Long postCount = 0L;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_breaking")
    @Builder.Default
    private Boolean isBreaking = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
