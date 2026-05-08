package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for poll data attached to a post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollResponse {

    private Long id;
    private String question;
    private Integer durationHours;
    private Integer totalVotes;
    private Boolean isClosed;
    private Boolean isExpired;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private List<PollOptionResponse> options;

    // User-specific
    private Long votedOptionId; // null if user hasn't voted
}
