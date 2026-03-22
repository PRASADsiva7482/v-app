package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.PollResponse;
import com.va.v.v_app.v.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for poll operations
 */
@RestController
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Polls", description = "Poll management APIs")
public class PollController {

    private final PollService pollService;

    @Operation(summary = "Vote on a poll")
    @PostMapping("/{pollId}/vote")
    public ResponseEntity<PollResponse> vote(
            @PathVariable Long pollId,
            @RequestParam Long optionId,
            Authentication authentication) {
        String userId = authentication.getName();
        PollResponse response = pollService.vote(pollId, optionId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get poll for a post")
    @GetMapping("/post/{postId}")
    public ResponseEntity<PollResponse> getPollForPost(
            @PathVariable Long postId,
            Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        PollResponse response = pollService.getPollForPost(postId, userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get poll by ID")
    @GetMapping("/{pollId}")
    public ResponseEntity<PollResponse> getPoll(
            @PathVariable Long pollId,
            Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        PollResponse response = pollService.getPollResponse(pollId, userId);
        return ResponseEntity.ok(response);
    }
}
