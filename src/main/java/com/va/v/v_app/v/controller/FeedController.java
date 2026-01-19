package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for feed operations
 */
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Feed", description = "Feed generation APIs")
public class FeedController {

    private final FeedService feedService;

    @Operation(summary = "Get timeline feed (posts from users you follow)")
    @GetMapping("/timeline")
    public ResponseEntity<Page<PostResponse>> getTimelineFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> feed = feedService.getTimelineFeed(userId, pageable);
        return ResponseEntity.ok(feed);
    }

    @Operation(summary = "Get explore feed (all public posts)")
    @GetMapping("/explore")
    public ResponseEntity<Page<PostResponse>> getExploreFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> feed = feedService.getExploreFeed(userId, pageable);
        return ResponseEntity.ok(feed);
    }

    @Operation(summary = "Get user's posts feed")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResponse>> getUserFeed(
            @PathVariable String userId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> feed = feedService.getUserFeed(userId, currentUserId, pageable);
        return ResponseEntity.ok(feed);
    }

    // ========== CURSOR-BASED PAGINATION (Infinite Scroll) ==========

    @Operation(summary = "Get timeline feed with cursor pagination (infinite scroll)")
    @GetMapping("/timeline/cursor")
    public ResponseEntity<com.va.v.v_app.v.dto.response.CursorPageResponse<PostResponse>> getTimelineFeedWithCursor(
            Authentication authentication,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        String userId = authentication.getName();
        var feed = feedService.getTimelineFeedWithCursor(userId, cursor, limit);
        return ResponseEntity.ok(feed);
    }

    @Operation(summary = "Get explore feed with cursor pagination (infinite scroll)")
    @GetMapping("/explore/cursor")
    public ResponseEntity<com.va.v.v_app.v.dto.response.CursorPageResponse<PostResponse>> getExploreFeedWithCursor(
            Authentication authentication,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        String userId = authentication != null ? authentication.getName() : null;
        var feed = feedService.getExploreFeedWithCursor(userId, cursor, limit);
        return ResponseEntity.ok(feed);
    }

    @Operation(summary = "Get user's posts feed with cursor pagination (infinite scroll)")
    @GetMapping("/user/{userId}/cursor")
    public ResponseEntity<com.va.v.v_app.v.dto.response.CursorPageResponse<PostResponse>> getUserFeedWithCursor(
            @PathVariable String userId,
            Authentication authentication,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        var feed = feedService.getUserFeedWithCursor(userId, currentUserId, cursor, limit);
        return ResponseEntity.ok(feed);
    }
}
