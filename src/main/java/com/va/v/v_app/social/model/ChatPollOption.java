package com.va.v.v_app.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_poll_option")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_poll_id", nullable = false)
    private ChatPoll chatPoll;

    @Column(name = "option_text", nullable = false, length = 200)
    private String optionText;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private Integer voteCount = 0;
}
