package com.va.v.v_app.social.api;

import com.va.v.v_app.social.dto.response.LikeResponse;
import com.va.v.v_app.social.service.LikeService;
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

/**
 * REST Controller for like operations
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Likes", description = "Like management APIs")
public class LikeController {

    private final LikeService likeService;

    @Operation(summary = "Like a post")
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Void> likePost(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication.getName();
        likeService.likePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unlike a post")
    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<Void> unlikePost(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication.getName();
        likeService.unlikePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get users who liked a post")
    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<Page<LikeResponse>> getPostLikes(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LikeResponse> likes = likeService.getPostLikes(postId, pageable);
        return ResponseEntity.ok(likes);
    }

    @Operation(summary = "Like a comment")
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        String userId = authentication.getName();
        likeService.likeComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unlike a comment")
    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        String userId = authentication.getName();
        likeService.unlikeComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get users who liked a comment")
    @GetMapping("/comments/{commentId}/likes")
    public ResponseEntity<Page<LikeResponse>> getCommentLikes(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LikeResponse> likes = likeService.getCommentLikes(commentId, pageable);
        return ResponseEntity.ok(likes);
    }
}
