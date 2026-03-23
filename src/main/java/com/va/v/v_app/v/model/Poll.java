package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a poll attached to a post
 */
@Entity
@Table(name = "poll", indexes = {
        @Index(name = "idx_poll_post_id", columnList = "post_id"),
        @Index(name = "idx_poll_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Column(name = "question", nullable = false, length = 500)
    private String question;

    @Column(name = "duration_hours", nullable = false)
    @Builder.Default
    private Integer durationHours = 24;

    @Column(name = "total_votes", nullable = false)
    @Builder.Default
    private Integer totalVotes = 0;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("position ASC")
    private List<PollOption> options = new ArrayList<>();
}
