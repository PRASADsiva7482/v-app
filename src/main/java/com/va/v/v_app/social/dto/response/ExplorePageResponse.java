package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the full Explore page data
 * Combines trending topics, news, trending hashtags, and trending posts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorePageResponse {

    private List<ExploreCategoryResponse> categories;
    private List<ExploreTopicResponse> trendingTopics;
    private List<ExploreNewsResponse> news;
    private List<HashtagResponse> trendingHashtags;
    private List<PostResponse> trendingPosts;
}
