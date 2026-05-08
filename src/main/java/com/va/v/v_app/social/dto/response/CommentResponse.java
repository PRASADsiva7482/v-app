package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private Long postId;
    private String userId;
    private Long parentCommentId;
    private String content;
    private Integer likesCount;
    private Integer repliesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related data
    private UserProfileResponse author;
    private List<CommentResponse> replies;

    // User-specific fields
    private Boolean isLiked; // True if current user liked this comment
    private Boolean isOwnComment; // True if current user is the author
}
