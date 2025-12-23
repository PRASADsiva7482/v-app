package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing follow relationship between users
 */
@Entity
@Table(name = "follow", uniqueConstraints = {
        @UniqueConstraint(name = "unique_follower_following", columnNames = { "follower_id", "following_id" })
}, indexes = {
        @Index(name = "idx_follower_id", columnList = "follower_id"),
        @Index(name = "idx_following_id", columnList = "following_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private String followerId; // User who follows

    @Column(name = "following_id", nullable = false)
    private String followingId; // User being followed

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
