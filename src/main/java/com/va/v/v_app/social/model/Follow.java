package com.va.v.v_app.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing follow relationship between users.
 * Supports follow requests for private profiles via the status field.
 */
@Entity
@Table(name = "follow", uniqueConstraints = {
        @UniqueConstraint(name = "unique_follower_following", columnNames = { "follower_id", "following_id" })
}, indexes = {
        @Index(name = "idx_follower_id", columnList = "follower_id"),
        @Index(name = "idx_following_id", columnList = "following_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_follow_status", columnList = "status"),
        @Index(name = "idx_following_status", columnList = "following_id, status")
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

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FollowStatus status = FollowStatus.ACCEPTED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Status of a follow relationship.
     * PENDING  – follow request sent to a private account, awaiting approval
     * ACCEPTED – follow is active (auto for public accounts, manual approve for private)
     * DECLINED – follow request was declined by the private account owner
     */
    public enum FollowStatus {
        PENDING,
        ACCEPTED,
        DECLINED
    }
}
