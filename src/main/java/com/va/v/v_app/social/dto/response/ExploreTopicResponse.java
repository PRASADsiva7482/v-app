package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for explore trending topic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreTopicResponse {

    private Long id;
    private String title;
    private String description;
    private String categoryName;
    private String categoryDisplayName;
    private String categoryIcon;
    private String location;
    private Long postCount;
    private Boolean isHashtag;
    private String hashtagName;
    private Boolean isPromoted;
    private Double trendingScore;
    private LocalDateTime startedTrendingAt;
    private LocalDateTime createdAt;
}
