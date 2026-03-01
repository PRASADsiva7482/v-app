package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.request.CreatePostRequest;
import com.va.v.v_app.v.dto.request.UpdatePostRequest;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.service.PostService;
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

/**
 * REST Controller for post operations
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "Post management APIs")
public class PostController {

    private final PostService postService;

    @Operation(summary = "Create a new post")
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        String userId = authentication.getName();
        PostResponse post = postService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @Operation(summary = "Get post by ID")
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        PostResponse post = postService.getPostById(postId, userId);
        return ResponseEntity.ok(post);
    }

    @Operation(summary = "Update a post")
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long postId,
            Authentication authentication,
            @Valid @RequestBody UpdatePostRequest request) {
        String userId = authentication.getName();
        PostResponse updated = postService.updatePost(postId, userId, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete a post")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication.getName();
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get posts by user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PostResponse>> getPostsByUser(
            @PathVariable String userId,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> posts = postService.getPostsByUser(userId, currentUserId, pageable);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "Search posts")
    @GetMapping("/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String keyword,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<PostResponse> posts = postService.searchPosts(keyword, currentUserId, pageable);
        return ResponseEntity.ok(posts);
    }

    @Operation(summary = "Increment view count")
    @PostMapping("/{postId}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long postId) {
        postService.incrementViewCount(postId);
        return ResponseEntity.ok().build();
    }
}
