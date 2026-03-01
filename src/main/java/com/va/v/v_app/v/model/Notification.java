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
 * Entity representing a user notification
 * Types: LIKE, COMMENT, FOLLOW, MENTION, REPOST, REPLY, SYSTEM
 */
@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_id, created_at"),
        @Index(name = "idx_notification_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notification_recipient_seen", columnList = "recipient_id, is_seen"),
        @Index(name = "idx_notification_type", columnList = "recipient_id, type"),
        @Index(name = "idx_notification_sender", columnList = "sender_id"),
        @Index(name = "idx_notification_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "is_seen")
    @Builder.Default
    private Boolean isSeen = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Notification types matching X/Twitter
     */
    public enum NotificationType {
        LIKE,
        COMMENT,
        FOLLOW,
        MENTION,
        REPOST,
        REPLY,
        SYSTEM
    }

    /**
     * Reference entity types
     */
    public enum ReferenceType {
        POST,
        COMMENT,
        USER
    }
}
