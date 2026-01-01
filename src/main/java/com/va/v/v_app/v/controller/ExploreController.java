package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.ExploreContentResponse;
import com.va.v.v_app.v.dto.response.TrendingTopicResponse;
import com.va.v.v_app.v.service.ExploreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Explore feature
 */
@RestController
@RequestMapping("/api/v1/explore")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Explore", description = "Explore and trending content APIs similar to Twitter/X")
public class ExploreController {

    private final ExploreService exploreService;

    /**
     * Get explore content for a specific category
     * Categories: FOR_YOU, TRENDING, NEWS, SPORTS, ENTERTAINMENT
     */
    @Operation(summary = "Get explore content by category", description = "Get trending topics, hashtags, posts, and suggested users by category")
    @GetMapping("/{category}")
    public ResponseEntity<ExploreContentResponse> getExploreContent(
            @Parameter(description = "Category: FOR_YOU, TRENDING, NEWS, SPORTS, ENTERTAINMENT") @PathVariable String category,
            @Parameter(description = "Number of items to return") @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {

        String currentUserId = authentication != null ? authentication.getName() : null;

        // Validate and normalize category
        String normalizedCategory = category.toUpperCase();
        if (!isValidCategory(normalizedCategory)) {
            normalizedCategory = "FOR_YOU";
        }

        ExploreContentResponse content = exploreService.getExploreContent(
                normalizedCategory, currentUserId, limit);

        log.info("Fetched explore content for category: {} by user: {}", normalizedCategory, currentUserId);
        return ResponseEntity.ok(content);
    }

    /**
     * Get all trending topics with pagination
     */
    @Operation(summary = "Get trending topics", description = "Get paginated list of trending topics, optionally filtered by category")
    @GetMapping("/trending-topics")
    public ResponseEntity<Page<TrendingTopicResponse>> getTrendingTopics(
            @Parameter(description = "Filter by category (optional)") @RequestParam(required = false) String category,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Page<TrendingTopicResponse> topics = exploreService.getTrendingTopics(category, page, size);

        log.info("Fetched trending topics - category: {}, page: {}, size: {}, total: {}",
                category, page, size, topics.getTotalElements());
        return ResponseEntity.ok(topics);
    }

    /**
     * Manually trigger trending score update (admin/debug endpoint)
     */
    @Operation(summary = "Update trending scores", description = "Manually trigger update of trending scores (normally runs automatically)")
    @PostMapping("/update-trending")
    public ResponseEntity<String> updateTrendingScores() {
        log.info("Manual trending score update triggered");
        exploreService.updateTrendingScores();
        return ResponseEntity.ok("Trending scores updated successfully");
    }

    /**
     * Validate category name
     */
    private boolean isValidCategory(String category) {
        return category.equals("FOR_YOU") ||
                category.equals("TRENDING") ||
                category.equals("NEWS") ||
                category.equals("SPORTS") ||
                category.equals("ENTERTAINMENT");
    }
}
