package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * Find all conversations for a user, ordered by most recently updated
     */
    @Query("SELECT DISTINCT c FROM Conversation c " +
            "JOIN c.participants p " +
            "WHERE p.userId = :userId AND p.leftAt IS NULL " +
            "ORDER BY c.updatedAt DESC")
    List<Conversation> findAllByUserId(@Param("userId") String userId);

    /**
     * Find existing DIRECT conversation between two users
     */
    @Query("SELECT c FROM Conversation c " +
            "JOIN c.participants p1 " +
            "JOIN c.participants p2 " +
            "WHERE c.type = 'DIRECT' " +
            "AND p1.userId = :user1 AND p1.leftAt IS NULL " +
            "AND p2.userId = :user2 AND p2.leftAt IS NULL")
    Optional<Conversation> findDirectConversation(
            @Param("user1") String user1,
            @Param("user2") String user2);
}
