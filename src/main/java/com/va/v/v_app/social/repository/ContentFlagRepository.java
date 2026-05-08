package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.ContentFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentFlagRepository extends JpaRepository<ContentFlag, Long> {
    List<ContentFlag> findByPostIdOrderByCreatedAtDesc(Long postId);
    List<ContentFlag> findByStatusOrderByCreatedAtDesc(String status);
    List<ContentFlag> findByReportedByOrderByCreatedAtDesc(String reportedBy);
    long countByPostIdAndStatus(Long postId, String status);
    boolean existsByPostIdAndReportedBy(Long postId, String reportedBy);
}
