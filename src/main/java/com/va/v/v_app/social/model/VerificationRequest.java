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
 * Entity representing a user's verification (checkmark) application request
 */
@Entity
@Table(name = "verification_request", indexes = {
        @Index(name = "idx_vr_user", columnList = "user_id"),
        @Index(name = "idx_vr_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // CREATOR, BUSINESS, GOVERNMENT, NEWS, ENTERTAINMENT, SPORTS, OTHER

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "document_url", length = 500)
    private String documentUrl; // ID proof upload URL

    // PENDING, APPROVED, REJECTED
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
