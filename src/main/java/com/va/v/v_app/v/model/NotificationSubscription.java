package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_subscription", indexes = {
        @Index(name = "idx_notif_sub_target", columnList = "target_user_id"),
        @Index(name = "idx_notif_sub_sub", columnList = "subscriber_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscriber_id", nullable = false)
    private String subscriberId;

    @Column(name = "target_user_id", nullable = false)
    private String targetUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
