package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for trending topics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingTopicResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private Long postCount;
    private Double trendScore;
    private String region;
    private String imageUrl;
    private String hashtagName; // If related to a hashtag
    private LocalDateTime trendingSince;
    private LocalDateTime createdAt;
}
