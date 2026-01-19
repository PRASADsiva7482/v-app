package com.va.v.v_app.v.repository;

import com.va.v.v_app.v.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Post entity
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

	Optional<Post> findByIdAndIsDeletedFalse(Long id);

	Page<Post> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

	Page<Post> findByIsDeletedFalse(Pageable pageable);

	// OPTIMIZED: Load post with media in single query
	@Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.mediaList WHERE p.id = :id AND p.isDeleted = false")
	Optional<Post> findByIdWithMedia(@Param("id") Long id);

	// OPTIMIZED: Load posts with media for feed (prevents N+1 queries for media)
	@Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.mediaList WHERE p.isDeleted = false AND p.userId IN :userIds ORDER BY p.createdAt DESC")
	List<Post> findByUserIdInWithMedia(@Param("userIds") List<String> userIds, Pageable pageable);

	// OPTIMIZED: Load user posts with media
	@Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.mediaList WHERE p.userId = :userId AND p.isDeleted = false ORDER BY p.createdAt DESC")
	List<Post> findByUserIdWithMedia(@Param("userId") String userId, Pageable pageable);

	// OPTIMIZED: Load explore feed with media
	@Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.mediaList WHERE p.isDeleted = false ORDER BY p.createdAt DESC")
	List<Post> findAllWithMedia(Pageable pageable);

	@Query("SELECT p FROM Post p WHERE p.userId IN :userIds AND p.isDeleted = false ORDER BY p.createdAt DESC")
	Page<Post> findByUserIdIn(@Param("userIds") List<String> userIds, Pageable pageable);

	@Modifying
	@Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
	void incrementViewCount(@Param("postId") Long postId);

	@Modifying
	@Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
	void incrementLikeCount(@Param("postId") Long postId);

	@Modifying
	@Query("UPDATE Post p SET p.likesCount = p.likesCount - 1 WHERE p.id = :postId AND p.likesCount > 0")
	void decrementLikeCount(@Param("postId") Long postId);

	@Modifying
	@Query("UPDATE Post p SET p.commentsCount = p.commentsCount + 1 WHERE p.id = :postId")
	void incrementCommentCount(@Param("postId") Long postId);

	@Modifying
	@Query("UPDATE Post p SET p.commentsCount = p.commentsCount - 1 WHERE p.id = :postId AND p.commentsCount > 0")
	void decrementCommentCount(@Param("postId") Long postId);

	// Additional methods for optimized feed service
	long countByUserIdAndIsDeletedFalse(String userId);

	@Query("SELECT COUNT(p) FROM Post p WHERE p.id IN :postIds")
	long countByPost_IdIn(@Param("postIds") List<Long> postIds);

	// Search posts by content or hashtags
	@Query("SELECT DISTINCT p FROM Post p " +
			"LEFT JOIN p.postHashtags ph " +
			"LEFT JOIN ph.hashtag h " +
			"WHERE p.isDeleted = false AND (" +
			"LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
			"LOWER(h.tagName) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
			") ORDER BY p.createdAt DESC")
	Page<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);

	// For You Feed - Recent posts query
	@Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.createdAt >= :since ORDER BY p.createdAt DESC")
	List<Post> findRecentPosts(@Param("since") java.time.LocalDateTime since, Pageable pageable);

	// For You Feed - Posts from followed users
	@Query("SELECT p FROM Post p WHERE p.userId IN :userIds AND p.isDeleted = false ORDER BY p.createdAt DESC")
	List<Post> findByUserIdInAndIsDeletedFalseOrderByCreatedAtDesc(@Param("userIds") List<String> userIds,
			Pageable pageable);

	// For You Feed - Posts with specific hashtags
	@Query("SELECT DISTINCT p FROM Post p " +
			"LEFT JOIN p.postHashtags ph " +
			"LEFT JOIN ph.hashtag h " +
			"WHERE p.isDeleted = false AND h.tagName IN :hashtagNames " +
			"ORDER BY p.createdAt DESC")
	List<Post> findByHashtagsIn(@Param("hashtagNames") java.util.Set<String> hashtagNames, Pageable pageable);

	// ========== CURSOR-BASED PAGINATION (Infinite Scroll) ==========

	/**
	 * Get posts with cursor-based pagination (all posts)
	 * Uses ID as cursor for efficient pagination without offset
	 */
	@Query("SELECT p FROM Post p WHERE p.isDeleted = false " +
			"AND (:cursor IS NULL OR p.id < :cursor) " +
			"ORDER BY p.id DESC")
	List<Post> findWithCursor(@Param("cursor") Long cursor, Pageable pageable);

	/**
	 * Get user's posts with cursor-based pagination
	 */
	@Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.isDeleted = false " +
			"AND (:cursor IS NULL OR p.id < :cursor) " +
			"ORDER BY p.id DESC")
	List<Post> findByUserIdWithCursor(@Param("userId") String userId, @Param("cursor") Long cursor, Pageable pageable);

	/**
	 * Get posts from followed users with cursor-based pagination
	 */
	@Query("SELECT p FROM Post p WHERE p.userId IN :userIds AND p.isDeleted = false " +
			"AND (:cursor IS NULL OR p.id < :cursor) " +
			"ORDER BY p.id DESC")
	List<Post> findByUserIdInWithCursor(@Param("userIds") List<String> userIds, @Param("cursor") Long cursor,
			Pageable pageable);

	/**
	 * Get posts with hashtags using cursor-based pagination
	 */
	@Query("SELECT DISTINCT p FROM Post p " +
			"LEFT JOIN p.postHashtags ph " +
			"LEFT JOIN ph.hashtag h " +
			"WHERE p.isDeleted = false AND h.tagName IN :hashtagNames " +
			"AND (:cursor IS NULL OR p.id < :cursor) " +
			"ORDER BY p.id DESC")
	List<Post> findByHashtagsInWithCursor(@Param("hashtagNames") java.util.Set<String> hashtagNames,
			@Param("cursor") Long cursor, Pageable pageable);

	/**
	 * Get recent posts with cursor-based pagination
	 */
	@Query("SELECT p FROM Post p WHERE p.isDeleted = false AND p.createdAt >= :since " +
			"AND (:cursor IS NULL OR p.id < :cursor) " +
			"ORDER BY p.id DESC")
	List<Post> findRecentPostsWithCursor(@Param("since") java.time.LocalDateTime since, @Param("cursor") Long cursor,
			Pageable pageable);
}
