package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.NotificationResponse;
import com.va.v.v_app.v.model.Notification.NotificationType;
import com.va.v.v_app.v.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Notifications
 * Similar to Twitter/X's notifications with All, Mentions tabs and read/unread
 * management
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "User notification APIs")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for the current user (paginated)
     */
    @GetMapping
    @Operation(summary = "Get notifications", description = "Returns paginated notifications for the current user")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Authentication authentication,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        String userId = authentication.getName();
        log.info("Fetching notifications for user: {}, page: {}, size: {}", userId, page, size);

        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications filtered by type
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "Get notifications by type", description = "Returns notifications filtered by type (LIKE, COMMENT, FOLLOW, MENTION, REPOST, REPLY, SYSTEM)")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByType(
            Authentication authentication,
            @PathVariable String type,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        String userId = authentication.getName();
        log.info("Fetching {} notifications for user: {}", type, userId);

        NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
        Page<NotificationResponse> notifications = notificationService
                .getNotificationsByType(userId, notificationType, page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get mentions for the current user
     */
    @GetMapping("/mentions")
    @Operation(summary = "Get mentions", description = "Returns notifications where the user was mentioned")
    public ResponseEntity<Page<NotificationResponse>> getMentions(
            Authentication authentication,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        String userId = authentication.getName();
        log.info("Fetching mentions for user: {}", userId);

        Page<NotificationResponse> mentions = notificationService.getMentions(userId, page, size);
        return ResponseEntity.ok(mentions);
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Returns the number of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String userId = authentication.getName();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Get unseen notification count (for bell badge)
     */
    @GetMapping("/unseen-count")
    @Operation(summary = "Get unseen count", description = "Returns the number of unseen notifications for the badge")
    public ResponseEntity<Map<String, Long>> getUnseenCount(Authentication authentication) {
        String userId = authentication.getName();
        long count = notificationService.getUnseenCount(userId);
        return ResponseEntity.ok(Map.of("unseenCount", count));
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/mark-all-read")
    @Operation(summary = "Mark all as read", description = "Marks all notifications as read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        String userId = authentication.getName();
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true, "updated", updated));
    }

    /**
     * Mark all notifications as seen
     */
    @PutMapping("/mark-all-seen")
    @Operation(summary = "Mark all as seen", description = "Marks all notifications as seen")
    public ResponseEntity<Map<String, Object>> markAllAsSeen(Authentication authentication) {
        String userId = authentication.getName();
        int updated = notificationService.markAllAsSeen(userId);
        return ResponseEntity.ok(Map.of("success", true, "updated", updated));
    }

    /**
     * Mark a specific notification as read
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    public ResponseEntity<Map<String, Boolean>> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId) {

        String userId = authentication.getName();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
