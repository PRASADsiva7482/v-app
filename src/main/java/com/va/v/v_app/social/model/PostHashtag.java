package com.va.v.v_app.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing the many-to-many relationship between posts and hashtags
 */
@Entity
@Table(name = "post_hashtag", indexes = {
        @Index(name = "idx_post_id", columnList = "post_id"),
        @Index(name = "idx_hashtag_id", columnList = "hashtag_id"),
        @Index(name = "idx_post_hashtag_unique", columnList = "post_id, hashtag_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
