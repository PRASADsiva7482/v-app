package com.va.v.v_app.v.controller;

import com.va.v.v_app.config.security.SecurityContextUtil;
import com.va.v.v_app.v.model.ContentFlag;
import com.va.v.v_app.v.service.ContentModerationService;
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
                    "flagId", flag.getId()
            ));
        }
        return ResponseEntity.ok(Map.of("flagged", false));
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
