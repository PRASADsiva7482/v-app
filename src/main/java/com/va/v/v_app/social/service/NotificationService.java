package com.va.v.v_app.social.service;

import com.va.v.v_app.social.dto.response.NotificationResponse;
import com.va.v.v_app.social.model.Notification;
import com.va.v.v_app.social.model.Notification.NotificationType;
import com.va.v.v_app.social.model.Notification.ReferenceType;
import com.va.v.v_app.social.model.Post;
import com.va.v.v_app.social.model.UserProfile;
import com.va.v.v_app.social.repository.NotificationRepository;
import com.va.v.v_app.social.repository.PostRepository;
import com.va.v.v_app.social.repository.UserProfileRepository;
import com.va.v.v_app.social.repository.FollowRepository;
import com.va.v.v_app.social.dto.UserSettingsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user notifications
 * Handles creation, retrieval, read/seen status, and enrichment with user
 * profiles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserProfileRepository userProfileRepository;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final UserSettingsService userSettingsService;

    /**
     * Create a notification (prevents duplicates for same sender+type+reference)
     */
    @Transactional
    public NotificationResponse createNotification(String recipientId, String senderId,
            NotificationType type, String message,
            Long referenceId, ReferenceType referenceType) {
        // Don't notify yourself
        if (recipientId != null && recipientId.equals(senderId)) {
            log.debug("Skipping self-notification for user: {}", recipientId);
            return null;
        }

        // Check for duplicate
        if (senderId != null && referenceId != null && referenceType != null) {
            boolean exists = notificationRepository
                    .existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndReferenceType(
                            recipientId, senderId, type, referenceId, referenceType);
            if (exists) {
                log.debug("Duplicate notification skipped: {} -> {} type={}", senderId, recipientId, type);
                return null;
            }
        }

        // Apply notification filters based on user settings
        if (recipientId != null && senderId != null) {
            UserSettingsDto settings = userSettingsService.getUserSettings(recipientId);

            // Mute accounts not following
            if (Boolean.TRUE.equals(settings.getMuteAccountsNotFollowing())) {
                boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(recipientId, senderId);
                if (!isFollowing) {
                    log.debug("Notification skipped due to settings (muteAccountsNotFollowing): {} -> {}", senderId,
                            recipientId);
                    return null;
                }
            }

            // Mute new accounts
            if (Boolean.TRUE.equals(settings.getMuteAccountsNew())) {
                UserProfile senderProfile = userProfileRepository.findByUserId(senderId).orElse(null);
                if (senderProfile != null && senderProfile.getCreatedAt() != null) {
                    // Treat accounts created in the last 7 days as new
                    if (senderProfile.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(7))) {
                        log.debug("Notification skipped due to settings (muteAccountsNew): {} -> {}", senderId,
                                recipientId);
                        return null;
                    }
                }
            }
        }

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .isSeen(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: {} -> {} type={}", senderId, recipientId, type);

        return enrichNotification(saved);
    }

    /**
     * Get notifications for a user (paginated)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, pageable);

        return enrichNotificationsPage(notifications);
    }

    /**
     * Get notifications filtered by type
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByType(String userId, NotificationType type,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository
                .findByRecipientIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);

        return enrichNotificationsPage(notifications);
    }

    /**
     * Get mentions for a user
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMentions(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository
                .findByRecipientIdAndTypeOrderByCreatedAtDesc(userId, NotificationType.MENTION, pageable);

        return enrichNotificationsPage(notifications);
    }

    /**
     * Get unread count (for badge)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    /**
     * Get unseen count (for notification bell badge)
     */
    @Transactional(readOnly = true)
    public long getUnseenCount(String userId) {
        return notificationRepository.countByRecipientIdAndIsSeenFalse(userId);
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public int markAllAsRead(String userId) {
        int updated = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for user: {}", updated, userId);
        return updated;
    }

    /**
     * Mark all notifications as seen
     */
    @Transactional
    public int markAllAsSeen(String userId) {
        int updated = notificationRepository.markAllAsSeen(userId);
        log.info("Marked {} notifications as seen for user: {}", updated, userId);
        return updated;
    }

    /**
     * Mark a single notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        notificationRepository.markAsRead(notificationId, userId);
    }

    // ==================== Helper Methods ====================

    /**
     * Convenience methods for creating specific notification types
     */
    @Transactional
    public void notifyLike(String recipientId, String senderId, Long postId) {
        createNotification(recipientId, senderId, NotificationType.LIKE,
                null, postId, ReferenceType.POST);
    }

    @Transactional
    public void notifyComment(String recipientId, String senderId, Long postId, String commentPreview) {
        String message = commentPreview != null && commentPreview.length() > 100
                ? commentPreview.substring(0, 100) + "..."
                : commentPreview;
        createNotification(recipientId, senderId, NotificationType.COMMENT,
                message, postId, ReferenceType.POST);
    }

    @Transactional
    public void notifyFollow(String recipientId, String senderId) {
        createNotification(recipientId, senderId, NotificationType.FOLLOW,
                null, null, ReferenceType.USER);
    }

    @Transactional
    public void notifyFollowRequest(String recipientId, String senderId) {
        createNotification(recipientId, senderId, NotificationType.FOLLOW_REQUEST,
                null, null, ReferenceType.USER);
    }

    @Transactional
    public void notifyFollowAccept(String recipientId, String senderId) {
        createNotification(recipientId, senderId, NotificationType.FOLLOW_ACCEPT,
                null, null, ReferenceType.USER);
    }

    @Transactional
    public void notifyMention(String recipientId, String senderId, Long postId) {
        createNotification(recipientId, senderId, NotificationType.MENTION,
                null, postId, ReferenceType.POST);
    }

    @Transactional
    public void notifyRepost(String recipientId, String senderId, Long postId) {
        createNotification(recipientId, senderId, NotificationType.REPOST,
                null, postId, ReferenceType.POST);
    }

    // ==================== Enrichment ====================

    /**
     * Enrich a page of notifications with sender profile info and reference content
     */
    private Page<NotificationResponse> enrichNotificationsPage(Page<Notification> notifications) {
        if (notifications.isEmpty()) {
            return notifications.map(this::mapToResponse);
        }

        // Collect unique sender IDs
        Set<String> senderIds = notifications.getContent().stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Collect post reference IDs
        Set<Long> postIds = notifications.getContent().stream()
                .filter(n -> n.getReferenceType() == ReferenceType.POST && n.getReferenceId() != null)
                .map(Notification::getReferenceId)
                .collect(Collectors.toSet());

        // Batch fetch user profiles
        Map<String, UserProfile> profileMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<UserProfile> profiles = userProfileRepository
                    .findByUserIdIn(new ArrayList<>(senderIds));
            profiles.forEach(p -> profileMap.put(p.getUserId(), p));
        }

        // Batch fetch posts for content preview
        Map<Long, Post> postMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<Post> posts = postRepository.findAllById(postIds);
            posts.forEach(p -> postMap.put(p.getId(), p));
        }

        return notifications.map(notification -> {
            NotificationResponse response = mapToResponse(notification);

            // Enrich with sender info
            if (notification.getSenderId() != null) {
                UserProfile sender = profileMap.get(notification.getSenderId());
                if (sender != null) {
                    response.setSenderUsername(sender.getUsername());
                    response.setSenderDisplayName(sender.getDisplayName());
                    response.setSenderProfilePictureUrl(sender.getProfilePictureUrl());
                }
            }

            // Enrich with reference content
            if (notification.getReferenceType() == ReferenceType.POST
                    && notification.getReferenceId() != null) {
                Post post = postMap.get(notification.getReferenceId());
                if (post != null) {
                    String content = post.getContent();
                    if (content != null && content.length() > 150) {
                        content = content.substring(0, 150) + "...";
                    }
                    response.setReferenceContent(content);
                }
            }

            return response;
        });
    }

    /**
     * Enrich a single notification
     */
    private NotificationResponse enrichNotification(Notification notification) {
        NotificationResponse response = mapToResponse(notification);

        if (notification.getSenderId() != null) {
            userProfileRepository.findByUserId(notification.getSenderId())
                    .ifPresent(sender -> {
                        response.setSenderUsername(sender.getUsername());
                        response.setSenderDisplayName(sender.getDisplayName());
                        response.setSenderProfilePictureUrl(sender.getProfilePictureUrl());
                    });
        }

        if (notification.getReferenceType() == ReferenceType.POST
                && notification.getReferenceId() != null) {
            postRepository.findById(notification.getReferenceId())
                    .ifPresent(post -> {
                        String content = post.getContent();
                        if (content != null && content.length() > 150) {
                            content = content.substring(0, 150) + "...";
                        }
                        response.setReferenceContent(content);
                    });
        }

        return response;
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .senderId(notification.getSenderId())
                .type(notification.getType().name())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType() != null
                        ? notification.getReferenceType().name()
                        : null)
                .isRead(notification.getIsRead())
                .isSeen(notification.getIsSeen())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
