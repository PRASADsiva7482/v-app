package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.service.NotificationSubscriptionService;
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
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Subscriptions", description = "Bell-icon notification subscription APIs")
public class SubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    @Operation(summary = "Subscribe to a user's notifications (bell icon)")
    @PostMapping("/{targetUserId}")
    public ResponseEntity<Map<String, Object>> subscribe(
            @PathVariable String targetUserId,
            Authentication authentication) {
        return ResponseEntity.ok(subscriptionService.subscribe(authentication.getName(), targetUserId));
    }

    @Operation(summary = "Unsubscribe from a user's notifications")
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable String targetUserId,
            Authentication authentication) {
        subscriptionService.unsubscribe(authentication.getName(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check subscription status")
    @GetMapping("/{targetUserId}/status")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @PathVariable String targetUserId,
            Authentication authentication) {
        boolean subscribed = subscriptionService.isSubscribed(authentication.getName(), targetUserId);
        return ResponseEntity.ok(Map.of("subscribed", subscribed));
    }

    @Operation(summary = "Get my subscriptions")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMySubscriptions(Authentication authentication) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(authentication.getName()));
    }
}
