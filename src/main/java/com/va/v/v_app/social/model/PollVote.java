package com.va.v.v_app.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a user's vote on a poll
 */
@Entity
@Table(name = "poll_vote", uniqueConstraints = {
        @UniqueConstraint(name = "unique_poll_user_vote", columnNames = { "poll_id", "user_id" })
}, indexes = {
        @Index(name = "idx_poll_vote_poll_id", columnList = "poll_id"),
        @Index(name = "idx_poll_vote_user_id", columnList = "user_id"),
        @Index(name = "idx_poll_vote_option_id", columnList = "option_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
