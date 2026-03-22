package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.MusicShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MusicShareRepository extends JpaRepository<MusicShare, Long> {
    List<MusicShare> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<MusicShare> findByPostId(Long postId);
    List<MusicShare> findTop20ByOrderByCreatedAtDesc();
}
