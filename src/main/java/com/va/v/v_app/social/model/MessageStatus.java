package com.va.v.v_app.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_status", uniqueConstraints = @UniqueConstraint(name = "uk_msg_user", columnNames = {
        "message_id", "user_id" }), indexes = {
                @Index(name = "idx_ms_user_id", columnList = "user_id"),
                @Index(name = "idx_ms_status", columnList = "status"),
                @Index(name = "idx_ms_msg_id", columnList = "message_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.SENT;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum DeliveryStatus {
        SENT, DELIVERED, READ
    }
}
