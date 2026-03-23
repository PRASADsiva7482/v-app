package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for bookmark operations
 */
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookmarks", description = "Bookmark/Save post APIs")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "Bookmark a post")
    @PostMapping("/{postId}")
    public ResponseEntity<Void> bookmarkPost(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication.getName();
        bookmarkService.bookmarkPost(postId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove bookmark from a post")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> unbookmarkPost(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication.getName();
        bookmarkService.unbookmarkPost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check if a post is bookmarked")
    @GetMapping("/{postId}/status")
    public ResponseEntity<Map<String, Boolean>> isBookmarked(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication.getName();
        boolean bookmarked = bookmarkService.isBookmarked(postId, userId);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    @Operation(summary = "Get user's bookmarked posts")
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getBookmarkedPosts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = bookmarkService.getBookmarkedPosts(userId, pageable);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "Get user's bookmark count")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getBookmarkCount(
            Authentication authentication) {
        String userId = authentication.getName();
        long count = bookmarkService.getBookmarkCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
