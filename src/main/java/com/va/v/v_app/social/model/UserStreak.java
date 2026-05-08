package com.va.v.v_app.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks user engagement streaks and gamification stats.
 * 
 * A streak increments each consecutive day the user performs
 * qualifying actions (post, like, comment, share).
 * If a day is skipped, the current streak resets to 0.
 */
@Entity
@Table(name = "user_streak", indexes = {
        @Index(name = "idx_streak_user_id", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "total_active_days", nullable = false)
    @Builder.Default
    private Integer totalActiveDays = 0;

    // Gamification points
    @Column(name = "xp_points", nullable = false)
    @Builder.Default
    private Long xpPoints = 0L;

    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 1;

    // Daily action counters (reset each day)
    @Column(name = "daily_posts", nullable = false)
    @Builder.Default
    private Integer dailyPosts = 0;

    @Column(name = "daily_likes", nullable = false)
    @Builder.Default
    private Integer dailyLikes = 0;

    @Column(name = "daily_comments", nullable = false)
    @Builder.Default
    private Integer dailyComments = 0;

    @Column(name = "last_daily_reset")
    private LocalDate lastDailyReset;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
