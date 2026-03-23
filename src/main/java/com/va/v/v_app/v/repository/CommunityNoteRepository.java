package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.CommunityNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityNoteRepository extends JpaRepository<CommunityNote, Long> {
    List<CommunityNote> findByPostIdOrderByCreatedAtDesc(Long postId);
    List<CommunityNote> findByPostIdAndStatusOrderByCreatedAtDesc(Long postId, String status);
    List<CommunityNote> findByAuthorIdOrderByCreatedAtDesc(String authorId);
}
