package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversationIdAndLeftAtIsNull(Long conversationId);

    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, String userId);

    @Query("SELECT cp.userId FROM ConversationParticipant cp " +
            "WHERE cp.conversation.id = :conversationId AND cp.leftAt IS NULL")
    List<String> findActiveUserIdsByConversationId(@Param("conversationId") Long conversationId);

    boolean existsByConversationIdAndUserIdAndLeftAtIsNull(Long conversationId, String userId);

    // Account deletion
    void deleteByUserId(String userId);
}
