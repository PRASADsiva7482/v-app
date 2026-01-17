package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.service.ForYouFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for "For You" personalized feed
 * High-performance, Redis-backed feed endpoint
 * 
 * Target: P95 latency < 100ms, Cache hit rate > 90%
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
@Tag(name = "For You Feed", description = "Personalized discovery feed APIs")
public class ForYouFeedController {

    private final ForYouFeedService forYouFeedService;

    /**
     * Get personalized "For You" feed
     * 
     * Feed composition:
     * - 40% Global trending
     * - 35% Following users
     * - 20% Interest-based (hashtags)
     * - 5% Discovery
     * 
     * @param authentication User authentication (optional for anonymous)
     * @param page           Page number (0-indexed)
     * @param size           Page size (max 100)
     * @return Personalized list of posts
     */
    @Operation(summary = "Get personalized 'For You' feed", description = "Returns a personalized feed based on trending content, followed users, "
            +
            "user interests, and discovery. Cached for optimal performance.")
    @GetMapping("/for-you")
    public ResponseEntity<Map<String, Object>> getForYouFeed(
            @Parameter(description = "Current user authentication", hidden = true) Authentication authentication,

            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Force refresh cache") @RequestParam(defaultValue = "false") boolean refresh) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate page size
            if (size > 100) {
                size = 100;
            }
            if (size < 1) {
                size = 20;
            }

            // Get user ID from authentication
            String userId = authentication != null ? authentication.getName() : null;

            log.info("For You feed request - user: {}, page: {}, size: {}, refresh: {}",
                    userId, page, size, refresh);

            // Generate feed
            List<PostResponse> posts = forYouFeedService.generateForYouFeed(userId, page, size);

            // Calculate metadata
            long duration = System.currentTimeMillis() - startTime;
            boolean cacheHit = duration < 50; // Heuristic: < 50ms likely cache hit

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", buildDataResponse(posts, page, size));
            response.put("metadata", buildMetadata(cacheHit, duration));

            log.info("For You feed generated in {}ms - {} posts returned", duration, posts.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating For You feed", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to generate feed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Build data response with pagination
     */
    private Map<String, Object> buildDataResponse(List<PostResponse> posts, int page, int size) {
        Map<String, Object> data = new HashMap<>();
        data.put("posts", posts);
        data.put("pagination", buildPagination(posts, page, size));
        return data;
    }

    /**
     * Build pagination metadata
     */
    private Map<String, Object> buildPagination(List<PostResponse> posts, int page, int size) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page);
        pagination.put("pageSize", size);
        pagination.put("itemsInPage", posts.size());
        pagination.put("hasNext", posts.size() == size); // Heuristic

        return pagination;
    }

    /**
     * Build response metadata
     */
    private Map<String, Object> buildMetadata(boolean cacheHit, long duration) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cacheHit", cacheHit);
        metadata.put("generationTime", duration + "ms");
        metadata.put("algorithm", "v1.0");
        metadata.put("timestamp", java.time.LocalDateTime.now().toString());

        return metadata;
    }

    /**
     * Health check endpoint for feed system
     */
    @Operation(summary = "Feed system health check")
    @GetMapping("/for-you/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "ForYouFeed");
        health.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(health);
    }
}
