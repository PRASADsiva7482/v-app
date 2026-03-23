package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_flag", indexes = {
        @Index(name = "idx_flag_post", columnList = "post_id"),
        @Index(name = "idx_flag_status", columnList = "status"),
        @Index(name = "idx_flag_type", columnList = "flag_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "reported_by")
    private String reportedBy;

    @Column(name = "flag_type", nullable = false, length = 30)
    private String flagType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "ai_categories", length = 500)
    private String aiCategories;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reviewer_id")
    private String reviewerId;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "action_taken", length = 30)
    private String actionTaken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
