package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserProfile entity
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByUsername(String username);

    boolean existsByUserId(String userId);

    boolean existsByUsername(String username);

    // Batch loading for multiple user IDs (prevents N+1 queries)
    List<UserProfile> findByUserIdIn(List<String> userIds);
}
