package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByUserIdAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(String userId, LocalDateTime now);
    List<Story> findByUserIdInAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(List<String> userIds, LocalDateTime now);
    List<Story> findByUserIdOrderByCreatedAtDesc(String userId);
}
