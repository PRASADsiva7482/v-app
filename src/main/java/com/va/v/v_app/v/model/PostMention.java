package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a user mentioned in a post
 */
@Entity
@Table(name = "post_mentions", indexes = {
        @Index(name = "idx_post_mentions_user", columnList = "mentioned_user_id"),
        @Index(name = "idx_post_mentions_post", columnList = "post_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "post_id", "mentioned_user_id" }, name = "uk_post_mention")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostMention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "mentioned_user_id", nullable = false)
    private String mentionedUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
