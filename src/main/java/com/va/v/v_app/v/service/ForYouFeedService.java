package com.va.v.v_app.v.service;

import com.va.v.v_app.config.feed.ForYouFeedConfig;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.model.ScoredPost;
import com.va.v.v_app.v.repository.FollowRepository;
import com.va.v.v_app.v.repository.PostHashtagRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for "For You" feed generation
 * Aggregates content from multiple sources and applies personalization
 * 
 * Feed Composition:
 * -40% Global Trending
 * - 35% Following
 * - 20% Interest-based
 * - 5% Discovery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForYouFeedService {

    private final ForYouFeedConfig config;
    private final RankingService rankingService;
    private final RedisForYouService redisService;
    private final PostService postService;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final PostHashtagRepository postHashtagRepository;

    /**
     * Generate personalized "For You" feed for a user
     * Note: Not read-only because PostService.mapToResponse may auto-create user
     * profiles
     */
    @Transactional
    public List<PostResponse> generateForYouFeed(String userId, int page, int size) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Check Redis cache first
            List<Long> cachedPostIds = redisService.getUserFeedPostIds(userId, size * 2); // Get more for pagination

            if (cachedPostIds != null && !cachedPostIds.isEmpty()) {
                log.debug("Cache HIT for user feed: {}", userId);
                return fetchAndConvertPosts(cachedPostIds, userId, page, size, true);
            }

            log.debug("Cache MISS for user feed: {}", userId);

            // 2. Generate fresh feed
            List<ScoredPost> scoredPosts = aggregateFeedSources(userId, size);

            // 3. Cache the feed
            if (!scoredPosts.isEmpty()) {
                redisService.cacheUserFeed(userId, scoredPosts);
            }

            // 4. Convert to response
            List<Long> postIds = scoredPosts.stream()
                    .map(ScoredPost::getPostId)
                    .collect(Collectors.toList());

            return fetchAndConvertPosts(postIds, userId, page, size, false);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("generateForYouFeed completed in {}ms for user: {}", duration, userId);
        }
    }

    /**
     * Generate personalized "For You" feed with cursor-based pagination
     * Optimized for infinite scroll
     * 
     * @param userId User ID
     * @param cursor Cursor for pagination (lastPostId), null for first page
     * @param limit  Number of posts to fetch
     * @return List of posts and next cursor
     */
    @Transactional
    public com.va.v.v_app.v.dto.response.CursorPageResponse<PostResponse> generateForYouFeedWithCursor(
            String userId, Long cursor, int limit) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate limit
            if (limit > 50) {
                limit = 50;
            }
            if (limit < 1) {
                limit = 20;
            }

            // Generate feed similar to original method but with cursor
            List<ScoredPost> scoredPosts = aggregateFeedSourcesWithCursor(userId, cursor, limit);

            // Convert to responses
            List<PostResponse> posts = scoredPosts.stream()
                    .map(sp -> {
                        Post post = postRepository.findById(sp.getPostId()).orElse(null);
                        return post != null ? postService.mapToResponse(post, userId) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Calculate next cursor
            String nextCursor = null;
            if (!posts.isEmpty() && posts.size() >= limit) {
                // Use the last post's ID as cursor
                nextCursor = String.valueOf(posts.get(posts.size() - 1).getId());
            }

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("generationTime", (System.currentTimeMillis() - startTime) + "ms");
            metadata.put("algorithm", "v1.0-cursor");
            metadata.put("timestamp", java.time.LocalDateTime.now().toString());

            return com.va.v.v_app.v.dto.response.CursorPageResponse.of(posts, nextCursor, limit, metadata);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("generateForYouFeedWithCursor completed in {}ms for user: {}", duration, userId);
        }
    }

    /**
     * Aggregate feed from multiple sources
     */
    private List<ScoredPost> aggregateFeedSources(String userId, int size) {
        var composition = config.getComposition();

        // Calculate how many posts from each source
        int trendingCount = (size * composition.getGlobalTrending()) / 100;
        int followingCount = (size * composition.getFollowing()) / 100;
        int interestCount = (size * composition.getInterestBased()) / 100;
        int discoveryCount = (size * composition.getDiscovery()) / 100;

        // Ensure we always get at least the requested size
        int total = trendingCount + followingCount + interestCount + discoveryCount;
        if (total < size) {
            trendingCount += (size - total);
        }

        log.debug("Feed composition - Trending: {}, Following: {}, Interest: {}, Discovery: {}",
                trendingCount, followingCount, interestCount, discoveryCount);

        // Fetch from each source
        List<ScoredPost> allPosts = new ArrayList<>();

        // 1. Global Trending
        allPosts.addAll(getGlobalTrendingPosts(trendingCount));

        // 2. Following Feed
        allPosts.addAll(getFollowingPosts(userId, followingCount));

        // 3. Interest-based
        allPosts.addAll(getInterestBasedPosts(userId, interestCount));

        // 4. Discovery
        allPosts.addAll(getDiscoveryPosts(userId, discoveryCount));

        // 5. Deduplicate (keep highest scored version)
        List<ScoredPost> uniquePosts = deduplicatePosts(allPosts);

        // 6. Rerank with personalization
        List<ScoredPost> rankedPosts = rerankWithPersonalization(uniquePosts, userId);

        // 7. Return top N
        return rankedPosts.stream()
                .limit(size * 2) // Cache more for pagination
                .collect(Collectors.toList());
    }

    /**
     * Get global trending posts from Redis (or fallback to DB)
     */
    private List<ScoredPost> getGlobalTrendingPosts(int count) {
        // Try Redis first
        List<Long> trendingIds = redisService.getGlobalTrendingPostIds(count);

        if (trendingIds != null && !trendingIds.isEmpty()) {
            return fetchPostsAndScore(trendingIds, null);
        }

        // Fallback to DB
        log.debug("Trending cache miss, falling back to DB");
        return getRecentPopularPosts(count);
    }

    /**
     * Aggregate feed from multiple sources with cursor-based pagination
     */
    private List<ScoredPost> aggregateFeedSourcesWithCursor(String userId, Long cursor, int limit) {
        var composition = config.getComposition();

        // Calculate how many posts from each source
        int trendingCount = (limit * composition.getGlobalTrending()) / 100;
        int followingCount = (limit * composition.getFollowing()) / 100;
        int interestCount = (limit * composition.getInterestBased()) / 100;
        int discoveryCount = (limit * composition.getDiscovery()) / 100;

        // Ensure we always get at least the requested size
        int total = trendingCount + followingCount + interestCount + discoveryCount;
        if (total < limit) {
            trendingCount += (limit - total);
        }

        log.debug("Feed composition with cursor - Trending: {}, Following: {}, Interest: {}, Discovery: {}",
                trendingCount, followingCount, interestCount, discoveryCount);

        // Fetch from each source with cursor
        List<ScoredPost> allPosts = new ArrayList<>();

        // 1. Global Trending
        allPosts.addAll(getGlobalTrendingPostsWithCursor(cursor, trendingCount));

        // 2. Following Feed
        allPosts.addAll(getFollowingPostsWithCursor(userId, cursor, followingCount));

        // 3. Interest-based
        allPosts.addAll(getInterestBasedPostsWithCursor(userId, cursor, interestCount));

        // 4. Discovery
        allPosts.addAll(getDiscoveryPostsWithCursor(userId, cursor, discoveryCount));

        // 5. Deduplicate (keep highest scored version)
        List<ScoredPost> uniquePosts = deduplicatePosts(allPosts);

        // 6. Rerank with personalization
        List<ScoredPost> rankedPosts = rerankWithPersonalization(uniquePosts, userId);

        // 7. Return top N
        return rankedPosts.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get global trending posts with cursor-based pagination
     */
    private List<ScoredPost> getGlobalTrendingPostsWithCursor(Long cursor, int count) {
        Pageable pageable = PageRequest.of(0, count);
        LocalDateTime since = LocalDateTime.now().minusDays(config.getRanking().getTimeDecay().getMaxAgeDays());

        List<Post> posts = postRepository.findRecentPostsWithCursor(since, cursor, pageable);

        return posts.stream()
                .filter(rankingService::meetsEngagementThreshold)
                .map(rankingService::calculateTrendingScore)
                .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Get following posts with cursor-based pagination
     */
    private List<ScoredPost> getFollowingPostsWithCursor(String userId, Long cursor, int count) {
        List<String> followingIds = followRepository.findFollowingUserIds(userId);

        if (followingIds.isEmpty()) {
            log.debug("User {} has no following, using global trending instead", userId);
            return getGlobalTrendingPostsWithCursor(cursor, count);
        }

        Pageable pageable = PageRequest.of(0, count);
        List<Post> posts = postRepository.findByUserIdInWithCursor(followingIds, cursor, pageable);

        return posts.stream()
                .map(rankingService::calculateTrendingScore)
                .collect(Collectors.toList());
    }

    /**
     * Get interest-based posts with cursor-based pagination
     */
    private List<ScoredPost> getInterestBasedPostsWithCursor(String userId, Long cursor, int count) {
        Set<String> interests = getUserInterests(userId);

        if (interests.isEmpty()) {
            log.debug("User {} has no interests, using discovery instead", userId);
            return getDiscoveryPostsWithCursor(userId, cursor, count);
        }

        // Extract hashtag names
        Set<String> hashtagNames = interests.stream()
                .filter(i -> i.startsWith("hashtag:"))
                .map(i -> i.replace("hashtag:", ""))
                .collect(Collectors.toSet());

        if (hashtagNames.isEmpty()) {
            return getDiscoveryPostsWithCursor(userId, cursor, count);
        }

        Pageable pageable = PageRequest.of(0, count);
        List<Post> posts = postRepository.findByHashtagsInWithCursor(hashtagNames, cursor, pageable);

        return posts.stream()
                .map(post -> rankingService.calculatePersonalizedScore(post, userId, interests))
                .collect(Collectors.toList());
    }

    /**
     * Get discovery posts with cursor-based pagination
     */
    private List<ScoredPost> getDiscoveryPostsWithCursor(String userId, Long cursor, int count) {
        Pageable pageable = PageRequest.of(0, count * 2);
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        List<Post> posts = postRepository.findRecentPostsWithCursor(since, cursor, pageable);

        // Shuffle for randomness
        Collections.shuffle(posts);

        return posts.stream()
                .limit(count)
                .map(rankingService::calculateTrendingScore)
                .collect(Collectors.toList());
    }

    /**
     * Get posts from users that the current user follows
     */
    private List<ScoredPost> getFollowingPosts(String userId, int count) {
        // Get following list
        List<String> followingIds = followRepository.findFollowingUserIds(userId);

        if (followingIds.isEmpty()) {
            log.debug("User {} has no following, using global trending instead", userId);
            return getGlobalTrendingPosts(count);
        }

        // Get recent posts from following
        Pageable pageable = PageRequest.of(0, count);
        List<Post> posts = postRepository.findByUserIdInAndIsDeletedFalseOrderByCreatedAtDesc(followingIds, pageable);

        return posts.stream()
                .map(rankingService::calculateTrendingScore)
                .collect(Collectors.toList());
    }

    /**
     * Get posts matching user interests
     */
    private List<ScoredPost> getInterestBasedPosts(String userId, int count) {
        // Get user interests from Redis or DB
        Set<String> interests = getUserInterests(userId);

        if (interests.isEmpty()) {
            log.debug("User {} has no interests, using discovery instead", userId);
            return getDiscoveryPosts(userId, count);
        }

        // Extract hashtag names
        Set<String> hashtagNames = interests.stream()
                .filter(i -> i.startsWith("hashtag:"))
                .map(i -> i.replace("hashtag:", ""))
                .collect(Collectors.toSet());

        if (hashtagNames.isEmpty()) {
            return getDiscoveryPosts(userId, count);
        }

        // Get posts with matching hashtags
        Pageable pageable = PageRequest.of(0, count);
        List<Post> posts = postRepository.findByHashtagsIn(hashtagNames, pageable);

        return posts.stream()
                .map(post -> rankingService.calculatePersonalizedScore(post, userId, interests))
                .collect(Collectors.toList());
    }

    /**
     * Get discovery posts (random,new content)
     */
    private List<ScoredPost> getDiscoveryPosts(String userId, int count) {
        // Get random recent posts
        Pageable pageable = PageRequest.of(0, count * 2);
        List<Post> recentPosts = postRepository.findRecentPosts(
                LocalDateTime.now().minusDays(7),
                pageable);

        // Shuffle for randomness
        Collections.shuffle(recentPosts);

        return recentPosts.stream()
                .limit(count)
                .map(rankingService::calculateTrendingScore)
                .collect(Collectors.toList());
    }

    /**
     * Deduplicate posts (keep version with highest score)
     */
    private List<ScoredPost> deduplicatePosts(List<ScoredPost> posts) {
        Map<Long, ScoredPost> uniquePosts = new HashMap<>();

        for (ScoredPost post : posts) {
            Long postId = post.getPostId();

            if (uniquePosts.containsKey(postId)) {
                ScoredPost existing = uniquePosts.get(postId);
                if (post.getFinalScore() > existing.getFinalScore()) {
                    uniquePosts.put(postId, post);
                }
            } else {
                uniquePosts.put(postId, post);
            }
        }

        return new ArrayList<>(uniquePosts.values());
    }

    /**
     * Rerank posts with personalization
     */
    private List<ScoredPost> rerankWithPersonalization(List<ScoredPost> posts, String userId) {
        return posts.stream()
                .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
                .collect(Collectors.toList());
    }

    /**
     * Fetch posts by IDs and convert to responses
     */
    private List<PostResponse> fetchAndConvertPosts(List<Long> postIds, String userId, int page, int size,
            boolean cacheHit) {
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, postIds.size());

        if (fromIndex >= postIds.size()) {
            return new ArrayList<>();
        }

        List<Long> pagePostIds = postIds.subList(fromIndex, toIndex);

        // Fetch posts from DB
        List<Post> posts = postRepository.findAllById(pagePostIds);

        // Maintain order from postIds
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));

        return pagePostIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .filter(post -> !post.getIsDeleted())
                .map(post -> postService.mapToResponse(post, userId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get user interests (from cache or DB)
     */
    private Set<String> getUserInterests(String userId) {
        // Try cache first
        Set<String> cachedInterests = redisService.getUserInterests(userId);
        if (cachedInterests != null) {
            return cachedInterests;
        }

        // Build from user activity
        Set<String> interests = new HashSet<>();

        // Get hashtags from user's recent posts
        Pageable pageable = PageRequest.of(0, 20);
        List<Post> userPosts = postRepository.findByUserIdAndIsDeletedFalse(userId, pageable).getContent();

        for (Post post : userPosts) {
            List<String> hashtags = postHashtagRepository.findHashtagsByPostId(post.getId())
                    .stream()
                    .map(h -> "hashtag:" + h.getTagName())
                    .toList();
            interests.addAll(hashtags);
        }

        // Cache for future use
        if (!interests.isEmpty()) {
            redisService.cacheUserInterests(userId, interests);
        }

        return interests;
    }

    /**
     * Get recent popular posts (fallback when cache is empty)
     */
    private List<ScoredPost> getRecentPopularPosts(int count) {
        LocalDateTime since = LocalDateTime.now().minusDays(config.getRanking().getTimeDecay().getMaxAgeDays());
        Pageable pageable = PageRequest.of(0, count * 2);

        List<Post> posts = postRepository.findRecentPosts(since, pageable);

        return posts.stream()
                .filter(rankingService::meetsEngagementThreshold)
                .map(rankingService::calculateTrendingScore)
                .sorted((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Fetch posts by IDs and calculate scores
     */
    private List<ScoredPost> fetchPostsAndScore(List<Long> postIds, String userId) {
        List<Post> posts = postRepository.findAllById(postIds);

        if (userId != null) {
            Set<String> interests = getUserInterests(userId);
            return posts.stream()
                    .map(post -> rankingService.calculatePersonalizedScore(post, userId, interests))
                    .collect(Collectors.toList());
        } else {
            return posts.stream()
                    .map(rankingService::calculateTrendingScore)
                    .collect(Collectors.toList());
        }
    }
}
