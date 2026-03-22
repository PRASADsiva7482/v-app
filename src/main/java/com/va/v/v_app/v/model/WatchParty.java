package com.va.v.v_app.v.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_party", indexes = {
        @Index(name = "idx_wp_host", columnList = "host_id"),
        @Index(name = "idx_wp_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_id", nullable = false)
    private String hostId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    @Column(name = "video_platform", length = 30)
    @Builder.Default
    private String videoPlatform = "CUSTOM";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "WAITING";

    @Column(name = "participant_count", nullable = false)
    @Builder.Default
    private Integer participantCount = 0;

    @Column(name = "max_participants")
    @Builder.Default
    private Integer maxParticipants = 50;

    @Column(name = "conversation_id")
    private Long conversationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
