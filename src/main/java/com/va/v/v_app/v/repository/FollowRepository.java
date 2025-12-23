package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Follow entity
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);

    // Get followers of a user
    Page<Follow> findByFollowingId(String followingId, Pageable pageable);

    // Get users that a user is following
    Page<Follow> findByFollowerId(String followerId, Pageable pageable);

    long countByFollowerId(String followerId); // Following count

    long countByFollowingId(String followingId); // Followers count

    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :userId")
    List<String> findFollowingUserIds(@Param("userId") String userId);
}
