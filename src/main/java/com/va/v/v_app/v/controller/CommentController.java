package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.request.CreateCommentRequest;
import com.va.v.v_app.v.dto.response.CommentResponse;
import com.va.v.v_app.v.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for comment operations
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "Comment management APIs")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Add comment to a post")
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            Authentication authentication,
            @Valid @RequestBody CreateCommentRequest request) {
        String userId = authentication.getName();
        CommentResponse comment = commentService.addComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @Operation(summary = "Get comments for a post")
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> getCommentsForPost(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CommentResponse> comments = commentService.getCommentsForPost(postId, currentUserId, pageable);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Reply to a comment")
    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<CommentResponse> replyToComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @Valid @RequestBody CreateCommentRequest request) {
        String userId = authentication.getName();
        CommentResponse reply = commentService.replyToComment(commentId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @Operation(summary = "Get replies for a comment")
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getRepliesForComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        List<CommentResponse> replies = commentService.getRepliesForComment(commentId, currentUserId);
        return ResponseEntity.ok(replies);
    }

    @Operation(summary = "Delete a comment")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        String userId = authentication.getName();
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    // ========== CURSOR-BASED PAGINATION (Infinite Scroll) ==========

    @Operation(summary = "Get comments for a post with cursor pagination (infinite scroll)")
    @GetMapping("/posts/{postId}/comments/cursor")
    public ResponseEntity<com.va.v.v_app.v.dto.response.CursorPageResponse<CommentResponse>> getCommentsWithCursor(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        var comments = commentService.getCommentsForPostWithCursor(postId, currentUserId, cursor, limit);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Get replies for a comment with cursor pagination (infinite scroll)")
    @GetMapping("/comments/{commentId}/replies/cursor")
    public ResponseEntity<com.va.v.v_app.v.dto.response.CursorPageResponse<CommentResponse>> getRepliesWithCursor(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        var replies = commentService.getRepliesForCommentWithCursor(commentId, currentUserId, cursor, limit);
        return ResponseEntity.ok(replies);
    }
}
