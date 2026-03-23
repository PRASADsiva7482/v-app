package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

        /**
         * Paginated message history for a conversation, newest first
         */
        Page<Message> findByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(
                        Long conversationId, Pageable pageable);

        /**
         * Find the latest message in a conversation (for chat list preview)
         */
        Optional<Message> findTopByConversationIdAndIsDeletedFalseOrderByCreatedAtDesc(Long conversationId);

        /**
         * Count unread messages for a user in a conversation
         */
        @Query("SELECT COUNT(m) FROM Message m " +
                        "JOIN MessageStatus ms ON ms.message.id = m.id " +
                        "WHERE m.conversationId = :conversationId " +
                        "AND ms.userId = :userId " +
                        "AND ms.status <> 'READ' " +
                        "AND m.senderId <> :userId " +
                        "AND m.isDeleted = false")
        long countUnreadMessages(
                        @Param("conversationId") Long conversationId,
                        @Param("userId") String userId);

        // Account deletion
        void deleteBySenderId(String senderId);

        // ========== VANISH MODE (Self-destructing messages) ==========

        /**
         * Soft-delete all messages whose expiry time has passed.
         * Used by MessageExpiryJob to clean up vanish-mode messages.
         */
        @org.springframework.data.jpa.repository.Modifying
        @Query("UPDATE Message m SET m.isDeleted = true " +
                        "WHERE m.expiresAt IS NOT NULL AND m.expiresAt <= :now AND m.isDeleted = false")
        int softDeleteExpiredMessages(@Param("now") java.time.LocalDateTime now);
}
