package com.va.v.v_app.social.api;

import com.va.v.v_app.core.security.SecurityContextUtil;
import com.va.v.v_app.social.model.ContentFlag;
import com.va.v.v_app.social.service.AdvancedModerationService;
import com.va.v.v_app.social.service.ContentModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
public class ContentModerationController {

    private final ContentModerationService contentModerationService;
    private final AdvancedModerationService advancedModerationService;

    @PostMapping("/report")
    public ResponseEntity<ContentFlag> reportContent(@RequestBody Map<String, String> request) {
        String userId = SecurityContextUtil.getCurrentUsername();
        Long postId = request.containsKey("postId") ? Long.parseLong(request.get("postId")) : null;
        String flagType = request.getOrDefault("flagType", "USER_REPORT");
        String reason = request.getOrDefault("reason", "");

        return ResponseEntity.ok(contentModerationService.reportContent(postId, userId, flagType, reason));
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanContent(@RequestBody Map<String, String> request) {
        String content = request.getOrDefault("content", "");
        Long postId = request.containsKey("postId") ? Long.parseLong(request.get("postId")) : null;

        ContentFlag flag = contentModerationService.scanContent(content, postId, null, null);

        if (flag != null) {
            return ResponseEntity.ok(Map.of(
                    "flagged", true,
                    "confidence", flag.getAiConfidence(),
                    "categories", flag.getAiCategories(),
                    "flagId", flag.getId()));
        }
        return ResponseEntity.ok(Map.of("flagged", false));
    }

    /**
     * Advanced AI-powered content scan with severity scoring and recommendations.
     * Returns detailed analysis including: severity, action, categories, PII, spam
     * score.
     */
    @PostMapping("/scan/advanced")
    public ResponseEntity<Map<String, Object>> advancedScan(@RequestBody Map<String, String> request) {
        String content = request.getOrDefault("content", "");

        AdvancedModerationService.ModerationResult result = advancedModerationService.analyzeContent(content);

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("flagged", result.isFlagged());
        response.put("severity", result.getSeverity());
        response.put("action", result.getAction());
        response.put("categories", result.getCategories());
        response.put("categoryConfidences", result.getCategoryConfidences());
        response.put("overallConfidence", result.getOverallConfidence());
        response.put("recommendations", result.getRecommendations());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ContentFlag>> getPendingFlags() {
        return ResponseEntity.ok(contentModerationService.getPendingFlags());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<ContentFlag>> getPostFlags(@PathVariable Long postId) {
        return ResponseEntity.ok(contentModerationService.getFlagsForPost(postId));
    }
}
