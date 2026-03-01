package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.HashtagResponse;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.service.HashtagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for hashtag-related endpoints
 */
@RestController
@RequestMapping("/api/v1/hashtags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Hashtags", description = "API endpoints for managing and searching hashtags")
public class HashtagController {

    private final HashtagService hashtagService;

    /**
     * Search hashtags by query
     */
    @GetMapping("/search")
    @Operation(summary = "Search hashtags", description = "Search hashtags by name prefix")
    public ResponseEntity<Page<HashtagResponse>> searchHashtags(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<HashtagResponse> hashtags = hashtagService.searchHashtags(query, pageable);

        log.info("Search hashtags with query: {}, found: {}", query, hashtags.getTotalElements());
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get trending hashtags
     */
    @GetMapping("/trending")
    @Operation(summary = "Get trending hashtags", description = "Get trending hashtags from the last 7 days")
    public ResponseEntity<Page<HashtagResponse>> getTrendingHashtags(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<HashtagResponse> hashtags = hashtagService.getTrendingHashtags(pageable);

        log.info("Get trending hashtags, found: {}", hashtags.getTotalElements());
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get top hashtags by usage count
     */
    @GetMapping("/top")
    @Operation(summary = "Get top hashtags", description = "Get most used hashtags")
    public ResponseEntity<Page<HashtagResponse>> getTopHashtags(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<HashtagResponse> hashtags = hashtagService.getTopHashtags(pageable);

        log.info("Get top hashtags, found: {}", hashtags.getTotalElements());
        return ResponseEntity.ok(hashtags);
    }

    /**
     * Get hashtag by name
     */
    @GetMapping("/{tagName}")
    @Operation(summary = "Get hashtag by name", description = "Get detailed information about a specific hashtag")
    public ResponseEntity<HashtagResponse> getHashtagByName(
            @Parameter(description = "Hashtag name (with or without #)") @PathVariable String tagName) {

        HashtagResponse hashtag = hashtagService.getHashtagByName(tagName);

        log.info("Get hashtag: {}", tagName);
        return ResponseEntity.ok(hashtag);
    }

    /**
     * Get posts by hashtag
     */
    @GetMapping("/{tagName}/posts")
    @Operation(summary = "Get posts by hashtag", description = "Get all posts containing a specific hashtag")
    public ResponseEntity<Page<PostResponse>> getPostsByHashtag(
            @Parameter(description = "Hashtag name (with or without #)") @PathVariable String tagName,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String currentUserId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = hashtagService.getPostsByHashtag(tagName, currentUserId, pageable);

        log.info("Get posts by hashtag: {}, found: {}", tagName, posts.getTotalElements());
        return ResponseEntity.ok(posts);
    }
}
