package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.service.CommunityNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/community-notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community Notes", description = "Community Notes / Fact-checking APIs")
public class CommunityNoteController {

    private final CommunityNoteService communityNoteService;

    @Operation(summary = "Add a community note to a post")
    @PostMapping("/post/{postId}")
    public ResponseEntity<Map<String, Object>> createNote(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = authentication.getName();
        String content = body.get("content");
        return ResponseEntity.ok(communityNoteService.createNote(postId, userId, content));
    }

    @Operation(summary = "Get all notes for a post")
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Map<String, Object>>> getNotesForPost(@PathVariable Long postId) {
        return ResponseEntity.ok(communityNoteService.getNotesForPost(postId));
    }

    @Operation(summary = "Get approved notes for a post")
    @GetMapping("/post/{postId}/approved")
    public ResponseEntity<List<Map<String, Object>>> getApprovedNotes(@PathVariable Long postId) {
        return ResponseEntity.ok(communityNoteService.getApprovedNotesForPost(postId));
    }

    @Operation(summary = "Vote on a community note")
    @PostMapping("/{noteId}/vote")
    public ResponseEntity<Map<String, Object>> voteOnNote(
            @PathVariable Long noteId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = authentication.getName();
        String voteType = body.get("voteType"); // UP or DOWN
        return ResponseEntity.ok(communityNoteService.voteOnNote(noteId, userId, voteType));
    }
}
