package com.va.v.v_app.v.controller;

import com.va.v.v_app.config.security.SecurityContextUtil;
import com.va.v.v_app.v.model.MusicShare;
import com.va.v.v_app.v.service.MusicShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicShareService musicShareService;

    @PostMapping("/share")
    public ResponseEntity<MusicShare> shareMusic(@RequestBody MusicShare musicShare) {
        String userId = SecurityContextUtil.getCurrentUsername();
        musicShare.setUserId(userId);
        return ResponseEntity.ok(musicShareService.shareMusic(musicShare));
    }

    @GetMapping("/me")
    public ResponseEntity<List<MusicShare>> getMyShares() {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(musicShareService.getUserShares(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MusicShare>> getUserShares(@PathVariable String userId) {
        return ResponseEntity.ok(musicShareService.getUserShares(userId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<MusicShare>> getRecentShares() {
        return ResponseEntity.ok(musicShareService.getRecentShares());
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<MusicShare> getByPost(@PathVariable Long postId) {
        return musicShareService.getByPostId(postId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShare(@PathVariable Long id) {
        String userId = SecurityContextUtil.getCurrentUsername();
        musicShareService.deleteShare(id, userId);
        return ResponseEntity.noContent().build();
    }
}
