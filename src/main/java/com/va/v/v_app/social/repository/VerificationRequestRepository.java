package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.VerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    Optional<VerificationRequest> findByUserIdAndStatus(String userId, String status);
    List<VerificationRequest> findByUserId(String userId);
    List<VerificationRequest> findByStatusOrderByCreatedAtAsc(String status);
}
