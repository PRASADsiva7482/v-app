package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.*;
import com.va.v.v_app.v.service.ExploreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for the Explore page
 * Similar to Twitter/X's Explore section with trending topics, news, and
 * categories
 */
@RestController
@RequestMapping("/api/v1/explore")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Explore", description = "Explore page APIs - trending topics, news, and categories")
public class ExploreController {

    private final ExploreService exploreService;

    /**
     * Get the full "For You" explore page data
     * Returns trending topics, news, trending hashtags, and trending posts in a
     * single call
     */
    @GetMapping("/for-you")
    @Operation(summary = "Get Explore 'For You' page", description = "Returns a combined view of trending topics, news, hashtags, and posts")
    public ResponseEntity<ExplorePageResponse> getForYouPage(
            Authentication authentication,
            @Parameter(description = "Number of trending topics") @RequestParam(defaultValue = "20") int topicLimit,
            @Parameter(description = "Number of news items") @RequestParam(defaultValue = "10") int newsLimit) {

        String userId = authentication != null ? authentication.getName() : null;
        log.info("Explore 'For You' page requested by user: {}", userId);

        ExplorePageResponse response = exploreService.getForYouPage(userId, topicLimit, newsLimit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active explore categories
     */
    @GetMapping("/categories")
    @Operation(summary = "Get explore categories", description = "Returns all active explore categories")
    public ResponseEntity<List<ExploreCategoryResponse>> getCategories() {
        log.info("Fetching explore categories");
        List<ExploreCategoryResponse> categories = exploreService.getCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get trending topics, optionally filtered by category
     */
    @GetMapping("/trending")
    @Operation(summary = "Get trending topics", description = "Returns trending topics, optionally filtered by category")
    public ResponseEntity<List<ExploreTopicResponse>> getTrendingTopics(
            @Parameter(description = "Category name filter") @RequestParam(required = false) String category,
            @Parameter(description = "Number of topics") @RequestParam(defaultValue = "20") int limit) {

        log.info("Fetching trending topics, category: {}, limit: {}", category, limit);

        List<ExploreTopicResponse> topics;
        if (category != null && !category.isEmpty()) {
            topics = exploreService.getTopicsByCategory(category, limit);
        } else {
            topics = exploreService.getTrendingTopics(limit);
        }

        return ResponseEntity.ok(topics);
    }

    /**
     * Get news items, optionally filtered by category
     */
    @GetMapping("/news")
    @Operation(summary = "Get news", description = "Returns news items, optionally filtered by category")
    public ResponseEntity<List<ExploreNewsResponse>> getNews(
            @Parameter(description = "Category name filter") @RequestParam(required = false) String category,
            @Parameter(description = "Number of news items") @RequestParam(defaultValue = "20") int limit) {

        log.info("Fetching news, category: {}, limit: {}", category, limit);
        List<ExploreNewsResponse> news = exploreService.getNews(category, limit);
        return ResponseEntity.ok(news);
    }

    /**
     * Get breaking news
     */
    @GetMapping("/news/breaking")
    @Operation(summary = "Get breaking news", description = "Returns current breaking news items")
    public ResponseEntity<List<ExploreNewsResponse>> getBreakingNews(
            @Parameter(description = "Number of items") @RequestParam(defaultValue = "5") int limit) {

        log.info("Fetching breaking news, limit: {}", limit);
        List<ExploreNewsResponse> breakingNews = exploreService.getBreakingNews(limit);
        return ResponseEntity.ok(breakingNews);
    }

    /**
     * Search explore topics
     */
    @GetMapping("/search")
    @Operation(summary = "Search explore topics", description = "Search trending topics by query")
    public ResponseEntity<List<ExploreTopicResponse>> searchTopics(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Number of results") @RequestParam(defaultValue = "20") int limit) {

        log.info("Searching explore topics with query: {}", query);
        List<ExploreTopicResponse> results = exploreService.searchTopics(query, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Get trending posts by category
     */
    @GetMapping("/trending/posts")
    @Operation(summary = "Get trending posts by category", description = "Returns trending posts, optionally filtered by category")
    public ResponseEntity<List<PostResponse>> getTrendingPostsByCategory(
            Authentication authentication,
            @Parameter(description = "Category name filter") @RequestParam(required = false) String category,
            @Parameter(description = "Number of posts") @RequestParam(defaultValue = "20") int limit) {

        String userId = authentication != null ? authentication.getName() : null;
        log.info("Fetching trending posts, category: {}, limit: {}", category, limit);

        List<PostResponse> posts = exploreService.getTrendingPostsByCategory(
                category, userId, limit);
        return ResponseEntity.ok(posts);
    }
}
