package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.Notification;
import com.va.v.v_app.social.model.Notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Notification entity
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

        // Get all notifications for a user (paginated, newest first)
        Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

        // Get notifications by type for a user
        Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(
                        String recipientId, NotificationType type, Pageable pageable);

        // Get unread notifications count
        long countByRecipientIdAndIsReadFalse(String recipientId);

        // Get unseen notifications count (for badge)
        long countByRecipientIdAndIsSeenFalse(String recipientId);

        // Get mentions only
        @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId " +
                        "AND n.type = 'MENTION' ORDER BY n.createdAt DESC")
        Page<Notification> findMentionsByRecipientId(
                        @Param("recipientId") String recipientId, Pageable pageable);

        // Mark all as read for a user
        @Modifying
        @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
        int markAllAsRead(@Param("recipientId") String recipientId);

        // Mark all as seen for a user
        @Modifying
        @Query("UPDATE Notification n SET n.isSeen = true WHERE n.recipientId = :recipientId AND n.isSeen = false")
        int markAllAsSeen(@Param("recipientId") String recipientId);

        // Mark a specific notification as read
        @Modifying
        @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipientId = :recipientId")
        int markAsRead(@Param("id") Long id, @Param("recipientId") String recipientId);

        // Check if duplicate notification exists (prevent spam)
        boolean existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndReferenceType(
                        String recipientId, String senderId, NotificationType type,
                        Long referenceId, Notification.ReferenceType referenceType);

        // Delete old notifications (cleanup)
        @Modifying
        @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
        int deleteOlderThan(@Param("before") java.time.LocalDateTime before);

        // Account deletion - delete all notifications for/from a user
        void deleteByRecipientId(String recipientId);

        void deleteBySenderId(String senderId);
}
