package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Explore page content
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreContentResponse {
    private List<TrendingTopicResponse> trendingTopics;
    private List<HashtagResponse> trendingHashtags;
    private List<PostResponse> trendingPosts;
    private List<UserSuggestionResponse> suggestedUsers;
    private PlatformStatsResponse platformStats;
    private String category; // FOR_YOU, TRENDING, NEWS, SPORTS, ENTERTAINMENT
}
