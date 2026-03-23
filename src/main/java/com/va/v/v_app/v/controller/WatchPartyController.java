package com.va.v.v_app.v.controller;

import com.va.v.v_app.config.security.SecurityContextUtil;
import com.va.v.v_app.v.model.WatchParty;
import com.va.v.v_app.v.service.WatchPartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watch-party")
@RequiredArgsConstructor
public class WatchPartyController {

    private final WatchPartyService watchPartyService;

    @PostMapping
    public ResponseEntity<WatchParty> createParty(@RequestBody WatchParty party) {
        String userId = SecurityContextUtil.getCurrentUsername();
        party.setHostId(userId);
        party.setStatus("ACTIVE");
        return ResponseEntity.ok(watchPartyService.createParty(party));
    }

    @GetMapping("/active")
    public ResponseEntity<List<WatchParty>> getActiveParties() {
        return ResponseEntity.ok(watchPartyService.getActiveParties());
    }

    @GetMapping("/popular")
    public ResponseEntity<List<WatchParty>> getPopularParties() {
        return ResponseEntity.ok(watchPartyService.getPopularParties());
    }

    @GetMapping("/me")
    public ResponseEntity<List<WatchParty>> getMyParties() {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(watchPartyService.getUserParties(userId));
    }

    @PostMapping("/{partyId}/join")
    public ResponseEntity<WatchParty> joinParty(@PathVariable Long partyId) {
        return ResponseEntity.ok(watchPartyService.joinParty(partyId));
    }

    @PostMapping("/{partyId}/leave")
    public ResponseEntity<WatchParty> leaveParty(@PathVariable Long partyId) {
        return ResponseEntity.ok(watchPartyService.leaveParty(partyId));
    }

    @PostMapping("/{partyId}/end")
    public ResponseEntity<Void> endParty(@PathVariable Long partyId) {
        String userId = SecurityContextUtil.getCurrentUsername();
        watchPartyService.endParty(partyId, userId);
        return ResponseEntity.noContent().build();
    }
}
