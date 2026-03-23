package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String userId;
    private String content;
    private Integer mediaCount;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer repostCount;
    private Integer viewsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related data
    private UserProfileResponse author;
    private List<MediaResponse> media;
    private List<HashtagResponse> hashtags;
    private List<String> mentionedUserIds;

    // User-specific fields
    private Boolean isLiked; // True if current user liked this post
    private Boolean isOwnPost; // True if current user is the author
    private Boolean isEditable; // True if the post is within the edit/delete time window for the author
    private Boolean isBookmarked; // True if current user bookmarked this post

    // Poll data (if the post has a poll)
    private PollResponse poll;

    private Boolean isDraft;
    private LocalDateTime scheduledFor;
}
