package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.ChatPoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatPollRepository extends JpaRepository<ChatPoll, Long> {
    Optional<ChatPoll> findByMessageId(Long messageId);
}
