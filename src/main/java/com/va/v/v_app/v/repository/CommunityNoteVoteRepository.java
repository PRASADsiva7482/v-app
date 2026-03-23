package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.CommunityNoteVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityNoteVoteRepository extends JpaRepository<CommunityNoteVote, Long> {
    Optional<CommunityNoteVote> findByNoteIdAndUserId(Long noteId, String userId);
    boolean existsByNoteIdAndUserId(Long noteId, String userId);
}
