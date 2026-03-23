package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Tag(name = "Stories", description = "24-hour ephemeral Stories/Fleets APIs")
public class StoryController {

    private final StoryService storyService;

    @Operation(summary = "Create a new story")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStory(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(storyService.createStory(userId,
                body.get("mediaUrl"), body.get("mediaType"), body.get("caption")));
    }

    @Operation(summary = "Get my active stories")
    @GetMapping("/me")
    public ResponseEntity<List<Map<String, Object>>> getMyStories(Authentication authentication) {
        return ResponseEntity.ok(storyService.getMyStories(authentication.getName()));
    }

    @Operation(summary = "Get feed stories (from followed users)")
    @GetMapping("/feed")
    public ResponseEntity<List<Map<String, Object>>> getFeedStories(Authentication authentication) {
        return ResponseEntity.ok(storyService.getFeedStories(authentication.getName()));
    }

    @Operation(summary = "View a story (increment view count)")
    @PostMapping("/{storyId}/view")
    public ResponseEntity<Void> viewStory(@PathVariable Long storyId) {
        storyService.viewStory(storyId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a story")
    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteStory(@PathVariable Long storyId, Authentication authentication) {
        storyService.deleteStory(storyId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
