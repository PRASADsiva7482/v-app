package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for gamification features: streaks, XP, badges.
 */
@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "Engagement streaks, XP, levels, and badges")
public class GamificationController {

    private final GamificationService gamificationService;

    @Operation(summary = "Get current user's gamification stats (streak, XP, level, badges)")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMyStats(Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(gamificationService.getUserStats(userId));
    }

    @Operation(summary = "Get another user's gamification stats")
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String userId) {
        return ResponseEntity.ok(gamificationService.getUserStats(userId));
    }

    @Operation(summary = "Manually record an action (POST, LIKE, COMMENT, SHARE)")
    @PostMapping("/action")
    public ResponseEntity<Map<String, Object>> recordAction(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = authentication.getName();
        String actionType = body.getOrDefault("actionType", "POST");
        return ResponseEntity.ok(gamificationService.recordAction(userId, actionType));
    }
}
