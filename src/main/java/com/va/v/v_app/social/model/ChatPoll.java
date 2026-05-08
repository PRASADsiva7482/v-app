package com.va.v.v_app.social.model;

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
 * Entity representing a poll sent inside a chat/DM message
 */
@Entity
@Table(name = "chat_poll", indexes = {
        @Index(name = "idx_chat_poll_msg", columnList = "message_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPoll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "question", nullable = false, length = 500)
    private String question;

    @Column(name = "total_votes", nullable = false)
    @Builder.Default
    private Integer totalVotes = 0;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chatPoll", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("position ASC")
    private List<ChatPollOption> options = new ArrayList<>();
}
