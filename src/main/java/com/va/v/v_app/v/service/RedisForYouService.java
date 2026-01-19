package com.va.v.v_app.v.service;

import com.va.v.v_app.config.feed.ForYouFeedConfig;
import com.va.v.v_app.v.model.ScoredPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for Redis operations related to "For You" feed
 * Handles ZSET (trending), HASH (scores), and SET (interests) operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisForYouService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ForYouFeedConfig config;

    // Redis Key Patterns
    private static final String TRENDING_GLOBAL_KEY = "trending:global:zset";
    private static final String USER_FEED_KEY = "feed:user:%s:zset";
    private static final String POST_SCORE_KEY = "post:%d:score";
    private static final String USER_INTERESTS_KEY = "user:%s:interests";
    private static final String USER_FOLLOWING_KEY = "user:%s:following";

    /**
     * Add post to global trending ZSET
     */
    public void addToGlobalTrending(ScoredPost scoredPost) {
        try {
            String key = TRENDING_GLOBAL_KEY;
            String member = "post:" + scoredPost.getPostId();
            double score = scoredPost.getTrendingScore();

            redisTemplate.opsForZSet().add(key, member, score);

            // Set TTL if not already set
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                int ttl = config.getCache().getGlobalTrending().getTtlSeconds();
                redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }

            log.debug("Added post {} to global trending with score {}", scoredPost.getPostId(), score);
        } catch (Exception e) {
            log.error("Error adding post to global trending: {}", scoredPost.getPostId(), e);
        }
    }

    /**
     * Get top N posts from global trending
     */
    public List<Long> getGlobalTrendingPostIds(int limit) {
        try {
            String key = TRENDING_GLOBAL_KEY;

            // Get top N posts (highest scores first)
            Set<Object> members = redisTemplate.opsForZSet()
                    .reverseRange(key, 0, limit - 1);

            if (members == null || members.isEmpty()) {
                log.debug("No posts in global trending cache");
                return new ArrayList<>();
            }

            return members.stream()
                    .map(member -> Long.parseLong(member.toString().replace("post:", "")))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting global trending posts", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get posts with scores from global trending
     */
    public List<ScoredPostWithId> getGlobalTrendingWithScores(int limit) {
        try {
            String key = TRENDING_GLOBAL_KEY;

            Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(key, 0, limit - 1);

            if (tuples == null || tuples.isEmpty()) {
                return new ArrayList<>();
            }

            return tuples.stream()
                    .map(tuple -> {
                        Long postId = Long.parseLong(tuple.getValue().toString().replace("post:", ""));
                        Double score = tuple.getScore();
                        return new ScoredPostWithId(postId, score);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting global trending with scores", e);
            return new ArrayList<>();
        }
    }

    /**
     * Cache user's personalized feed
     */
    public void cacheUserFeed(String userId, List<ScoredPost> posts) {
        try {
            String key = String.format(USER_FEED_KEY, userId);

            // Delete existing
            redisTemplate.delete(key);

            // Add all posts
            for (ScoredPost post : posts) {
                String member = "post:" + post.getPostId();
                double score = post.getFinalScore();
                redisTemplate.opsForZSet().add(key, member, score);
            }

            // Set TTL
            int ttl = config.getCache().getUserFeed().getTtlSeconds();
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);

            log.debug("Cached feed for user {} with {} posts", userId, posts.size());
        } catch (Exception e) {
            log.error("Error caching user feed for user: {}", userId, e);
        }
    }

    /**
     * Get cached user feed
     */
    public List<Long> getUserFeedPostIds(String userId, int limit) {
        try {
            String key = String.format(USER_FEED_KEY, userId);

            Set<Object> members = redisTemplate.opsForZSet()
                    .reverseRange(key, 0, limit - 1);

            if (members == null || members.isEmpty()) {
                log.debug("No cached feed for user: {}", userId);
                return null; // Null indicates cache miss
            }

            return members.stream()
                    .map(member -> Long.parseLong(member.toString().replace("post:", "")))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting user feed for user: {}", userId, e);
            return null;
        }
    }

    /**
     * Cache post score details
     */
    public void cachePostScore(ScoredPost scoredPost) {
        try {
            String key = String.format(POST_SCORE_KEY, scoredPost.getPostId());

            Map<String, String> scoreData = new HashMap<>();
            scoreData.put("engagement_score", String.valueOf(scoredPost.getEngagementScore()));
            scoreData.put("trending_score", String.valueOf(scoredPost.getTrendingScore()));
            scoreData.put("velocity", String.valueOf(scoredPost.getEngagementVelocity()));
            scoreData.put("calculated_at", scoredPost.getScoreCalculatedAt().toString());
            scoreData.put("likes", String.valueOf(scoredPost.getLikesCount()));
            scoreData.put("comments", String.valueOf(scoredPost.getCommentsCount()));

            redisTemplate.opsForHash().putAll(key, scoreData);

            // Set TTL
            int ttl = config.getCache().getPostScores().getTtlSeconds();
            redisTemplate.expire(key, ttl, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Error caching post score: {}", scoredPost.getPostId(), e);
        }
    }

    /**
     * Get cached post score
     */
    public Map<String, Object> getCachedPostScore(Long postId) {
        try {
            String key = String.format(POST_SCORE_KEY, postId);
            @SuppressWarnings("unchecked")
            Map<String, Object> entries = (Map<String, Object>) (Map<?, ?>) redisTemplate.opsForHash().entries(key);
            return entries;
        } catch (Exception e) {
            log.error("Error getting cached post score: {}", postId, e);
            return null;
        }
    }

    /**
     * Cache user interests
     */
    public void cacheUserInterests(String userId, Set<String> interests) {
        try {
            String key = String.format(USER_INTERESTS_KEY, userId);

            // Clear existing
            redisTemplate.delete(key);

            // Add interests
            if (interests != null && !interests.isEmpty()) {
                redisTemplate.opsForSet().add(key, interests.toArray());

                // Set TTL
                int ttl = config.getCache().getUserInterests().getTtlSeconds();
                redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }

            log.debug("Cached {} interests for user {}", interests != null ? interests.size() : 0, userId);
        } catch (Exception e) {
            log.error("Error caching user interests: {}", userId, e);
        }
    }

    /**
     * Get cached user interests
     */
    public Set<String> getUserInterests(String userId) {
        try {
            String key = String.format(USER_INTERESTS_KEY, userId);
            Set<Object> members = redisTemplate.opsForSet().members(key);

            if (members == null) {
                return null;
            }

            return members.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("Error getting user interests: {}", userId, e);
            return null;
        }
    }

    /**
     * Cache user following list
     */
    public void cacheUserFollowing(String userId, Set<String> followingIds) {
        try {
            String key = String.format(USER_FOLLOWING_KEY, userId);

            // Clear existing
            redisTemplate.delete(key);

            // Add following
            if (followingIds != null && !followingIds.isEmpty()) {
                redisTemplate.opsForSet().add(key, followingIds.toArray());

                // Set TTL
                redisTemplate.expire(key, 3600, TimeUnit.SECONDS); // 1 hour
            }

            log.debug("Cached {} following for user {}", followingIds != null ? followingIds.size() : 0, userId);
        } catch (Exception e) {
            log.error("Error caching user following: {}", userId, e);
        }
    }

    /**
     * Get cached user following list
     */
    public Set<String> getUserFollowing(String userId) {
        try {
            String key = String.format(USER_FOLLOWING_KEY, userId);
            Set<Object> members = redisTemplate.opsForSet().members(key);

            if (members == null) {
                return null;
            }

            return members.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("Error getting user following: {}", userId, e);
            return null;
        }
    }

    /**
     * Clear all trending data (for refresh)
     */
    public void clearGlobalTrending() {
        try {
            redisTemplate.delete(TRENDING_GLOBAL_KEY);
            log.info("Cleared global trending cache");
        } catch (Exception e) {
            log.error("Error clearing global trending", e);
        }
    }

    /**
     * Remove old posts from trending (older than max age)
     */
    public void removeOldPosts(long maxAgeMillis) {
        try {
            // Remove posts with score < threshold (can be based on time)
            // For now, we'll let TTL handle this
            // In production, you might want to actively remove based on post age
            log.debug("removeOldPosts called with maxAge: {}ms", maxAgeMillis);

        } catch (Exception e) {
            log.error("Error removing old posts", e);
        }
    }

    /**
     * Helper class for post ID with score
     */
    public static class ScoredPostWithId {
        public final Long postId;
        public final Double score;

        public ScoredPostWithId(Long postId, Double score) {
            this.postId = postId;
            this.score = score;
        }
    }
}
