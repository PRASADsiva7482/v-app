package com.va.v.v_app.social.api;

import com.va.v.v_app.social.dto.response.CursorPageResponse;
import com.va.v.v_app.social.dto.response.PostResponse;
import com.va.v.v_app.social.service.ForYouFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for "For You" personalized feed
 * 
 * Fixes applied:
 * - B-4: Removed generic Exception catch — let GlobalExceptionHandler handle it
 * - B-8: Page size validation centralized
 * - B-14: Removed /health test endpoint (use actuator instead)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
@Tag(name = "For You Feed", description = "Personalized discovery feed APIs")
public class ForYouFeedController {

    private final ForYouFeedService forYouFeedService;

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_CURSOR_LIMIT = 50;

    /**
     * Get personalized "For You" feed
     */
    @Operation(summary = "Get personalized 'For You' feed")
    @GetMapping("/for-you")
    public ResponseEntity<Map<String, Object>> getForYouFeed(
            @Parameter(description = "Current user authentication", hidden = true) Authentication authentication,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Force refresh cache") @RequestParam(defaultValue = "false") boolean refresh) {

        long startTime = System.currentTimeMillis();

        // B-8: Centralized page size validation
        size = clampPageSize(size, MAX_PAGE_SIZE, DEFAULT_PAGE_SIZE);

        String userId = authentication != null ? authentication.getName() : null;

        log.info("For You feed request - user: {}, page: {}, size: {}, refresh: {}",
                userId, page, size, refresh);

        // B-4: No try-catch — exceptions propagate to GlobalExceptionHandler
        List<PostResponse> posts = forYouFeedService.generateForYouFeed(userId, page, size);

        long duration = System.currentTimeMillis() - startTime;
        boolean cacheHit = duration < 50;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", buildDataResponse(posts, page, size));
        response.put("metadata", buildMetadata(cacheHit, duration));

        log.info("For You feed generated in {}ms - {} posts returned", duration, posts.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get personalized "For You" feed with cursor-based pagination (Infinite
     * Scroll)
     */
    @Operation(summary = "Get 'For You' feed with cursor pagination")
    @GetMapping("/for-you/cursor")
    public ResponseEntity<CursorPageResponse<PostResponse>> getForYouFeedWithCursor(
            @Parameter(description = "Current user authentication", hidden = true) Authentication authentication,
            @Parameter(description = "Cursor for next page") @RequestParam(required = false) Long cursor,
            @Parameter(description = "Number of posts per page (max 50)") @RequestParam(defaultValue = "20") int limit) {

        // B-8: Centralized limit validation
        limit = clampPageSize(limit, MAX_CURSOR_LIMIT, DEFAULT_PAGE_SIZE);

        String userId = authentication != null ? authentication.getName() : null;
        log.info("For You feed cursor request - user: {}, cursor: {}, limit: {}", userId, cursor, limit);

        long startTime = System.currentTimeMillis();

        // B-4: No try-catch — GlobalExceptionHandler handles errors
        var response = forYouFeedService.generateForYouFeedWithCursor(userId, cursor, limit);

        long duration = System.currentTimeMillis() - startTime;
        log.info("For You feed cursor generated in {}ms - {} posts returned", duration,
                response.getData().size());

        return ResponseEntity.ok(response);
    }

    // B-14: REMOVED /for-you/health test endpoint — use /actuator/health instead

    /**
     * B-8: Centralized page size clamping
     */
    private int clampPageSize(int size, int max, int defaultSize) {
        if (size > max)
            return max;
        if (size < 1)
            return defaultSize;
        return size;
    }

    private Map<String, Object> buildDataResponse(List<PostResponse> posts, int page, int size) {
        Map<String, Object> data = new HashMap<>();
        data.put("posts", posts);
        data.put("pagination", buildPagination(posts, page, size));
        return data;
    }

    private Map<String, Object> buildPagination(List<PostResponse> posts, int page, int size) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page);
        pagination.put("pageSize", size);
        pagination.put("itemsInPage", posts.size());
        pagination.put("hasNext", posts.size() == size);
        return pagination;
    }

    private Map<String, Object> buildMetadata(boolean cacheHit, long duration) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cacheHit", cacheHit);
        metadata.put("generationTime", duration + "ms");
        metadata.put("algorithm", "v1.0");
        metadata.put("timestamp", LocalDateTime.now().toString());
        return metadata;
    }
}
