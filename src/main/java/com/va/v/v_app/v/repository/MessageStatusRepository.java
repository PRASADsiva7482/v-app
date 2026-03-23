package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {

        List<MessageStatus> findByMessageId(Long messageId);

        /**
         * Get the "worst" status for a message across all recipients.
         * If any recipient hasn't read, the overall status is DELIVERED or SENT.
         */
        @Query("SELECT ms FROM MessageStatus ms WHERE ms.message.id = :messageId AND ms.userId = :userId")
        MessageStatus findByMessageIdAndUserId(
                        @Param("messageId") Long messageId,
                        @Param("userId") String userId);

        /**
         * Mark all messages in a conversation as DELIVERED for a user
         */
        @Modifying
        @Query("UPDATE MessageStatus ms SET ms.status = 'DELIVERED', ms.deliveredAt = :now " +
                        "WHERE ms.userId = :userId " +
                        "AND ms.status = 'SENT' " +
                        "AND ms.message.id IN (" +
                        "  SELECT m.id FROM Message m WHERE m.conversationId = :conversationId" +
                        ")")
        int markAsDelivered(
                        @Param("conversationId") Long conversationId,
                        @Param("userId") String userId,
                        @Param("now") LocalDateTime now);

        /**
         * Mark all messages in a conversation as READ for a user
         */
        @Modifying
        @Query("UPDATE MessageStatus ms SET ms.status = 'READ', ms.readAt = :now " +
                        "WHERE ms.userId = :userId " +
                        "AND ms.status IN ('SENT', 'DELIVERED') " +
                        "AND ms.message.id IN (" +
                        "  SELECT m.id FROM Message m WHERE m.conversationId = :conversationId AND m.senderId <> :userId"
                        +
                        ")")
        int markAsRead(
                        @Param("conversationId") Long conversationId,
                        @Param("userId") String userId,
                        @Param("now") LocalDateTime now);

        /**
         * Find the worst status for a message across all recipients
         */
        @Query("SELECT MIN(CASE ms.status " +
                        "  WHEN 'SENT' THEN 0 " +
                        "  WHEN 'DELIVERED' THEN 1 " +
                        "  WHEN 'READ' THEN 2 END) " +
                        "FROM MessageStatus ms WHERE ms.message.id = :messageId")
        Integer findWorstStatusForMessage(@Param("messageId") Long messageId);

        // Account deletion
        void deleteByUserId(String userId);
}
