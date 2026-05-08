package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.Follow;
import com.va.v.v_app.social.model.Follow.FollowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Follow entity.
 * Supports status-aware queries for public/private profile follow requests.
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

    // Account deletion - remove all follow relationships
    void deleteByFollowerId(String followerId);

    void deleteByFollowingId(String followingId);

    // ==================== Status-aware queries for private profiles ====================

    /**
     * Check if an ACCEPTED follow relationship exists between two users.
     * This is the main check used throughout the app to determine if a user is truly following another.
     */
    boolean existsByFollowerIdAndFollowingIdAndStatus(String followerId, String followingId, FollowStatus status);

    /**
     * Get accepted followers of a user (excludes PENDING/DECLINED).
     */
    Page<Follow> findByFollowingIdAndStatus(String followingId, FollowStatus status, Pageable pageable);

    /**
     * Get accepted following for a user (excludes PENDING/DECLINED).
     */
    Page<Follow> findByFollowerIdAndStatus(String followerId, FollowStatus status, Pageable pageable);

    /**
     * Get IDs of users that a user is actively following (ACCEPTED only).
     */
    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :userId AND f.status = 'ACCEPTED'")
    List<String> findAcceptedFollowingUserIds(@Param("userId") String userId);

    /**
     * Get pending follow requests FOR a user (requests they need to approve/decline).
     * These are people who want to follow this private account.
     */
    Page<Follow> findByFollowingIdAndStatusOrderByCreatedAtDesc(String followingId, FollowStatus status, Pageable pageable);

    /**
     * Count pending follow requests for a user.
     */
    long countByFollowingIdAndStatus(String followingId, FollowStatus status);

    /**
     * Count accepted followers for a user.
     */
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followingId = :userId AND f.status = 'ACCEPTED'")
    long countAcceptedFollowers(@Param("userId") String userId);

    /**
     * Count accepted following for a user.
     */
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followerId = :userId AND f.status = 'ACCEPTED'")
    long countAcceptedFollowing(@Param("userId") String userId);
}

