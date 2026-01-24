package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.PlatformStatsResponse;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.dto.response.UserSuggestionResponse;
import com.va.v.v_app.v.dto.response.SmartSuggestionResponse;
import com.va.v.v_app.v.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for discovery and trending features
 */
@RestController
@RequestMapping("/api/v1/discovery")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Discovery", description = "Discovery and trending content APIs")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @Operation(summary = "Get trending posts based on engagement score")
    @GetMapping("/trending/posts")
    public ResponseEntity<List<PostResponse>> getTrendingPosts(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = authentication != null ? authentication.getName() : null;
        List<PostResponse> trending = discoveryService.getTrendingPosts(userId, limit);
        return ResponseEntity.ok(trending);
    }

    @Operation(summary = "Get popular users to follow based on popularity score")
    @GetMapping("/popular/users")
    public ResponseEntity<List<UserSuggestionResponse>> getPopularUsers(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = authentication != null ? authentication.getName() : null;
        List<UserSuggestionResponse> popular = discoveryService.getPopularUsers(userId, limit);
        return ResponseEntity.ok(popular);
    }

    @Operation(summary = "Get smart user suggestions based on social network")
    @GetMapping("/suggestions/users")
    public ResponseEntity<List<SmartSuggestionResponse>> getSmartSuggestions(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = authentication != null ? authentication.getName() : null;
        List<SmartSuggestionResponse> suggestions = discoveryService.getSmartUserSuggestions(userId, limit);
        return ResponseEntity.ok(suggestions);
    }

    @Operation(summary = "Get platform statistics")
    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsResponse> getPlatformStats() {
        PlatformStatsResponse stats = discoveryService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }
}
