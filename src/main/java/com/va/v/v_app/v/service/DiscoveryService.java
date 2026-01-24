package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.PlatformStatsResponse;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.dto.response.UserSuggestionResponse;
import com.va.v.v_app.v.dto.response.SmartSuggestionResponse;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.model.UserProfile;
import com.va.v.v_app.v.model.Follow;
import com.va.v.v_app.v.repository.FollowRepository;
import com.va.v.v_app.v.repository.PostRepository;
import com.va.v.v_app.v.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for discovery features: trending posts, popular users, platform stats
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class DiscoveryService {

    private final PostRepository postRepository;
    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;
    private final PostService postService;

    /**
     * Get trending posts using engagement score algorithm
     * Algorithm: Score = (likes * 2) + (comments * 3) + (views * 0.1) +
     * recency_bonus
     * Recency bonus: Posts within last 24h get +100, last 7 days get +50
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getTrendingPosts(String currentUserId, int limit) {
        log.debug("Fetching trending posts with limit: {}", limit);

        try {
            // Fetch recent posts (last 30 days) with high engagement
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            Pageable pageable = PageRequest.of(0, 100); // Get top 100 to calculate scores

            List<Post> allPosts = postRepository.findAllWithMedia(pageable);
            if (allPosts == null || allPosts.isEmpty()) {
                log.debug("No posts found in database");
                return new ArrayList<>();
            }

            List<Post> recentPosts = allPosts.stream()
                    .filter(post -> post != null && post.getCreatedAt() != null)
                    .filter(post -> post.getCreatedAt().isAfter(thirtyDaysAgo))
                    .collect(Collectors.toList());

            if (recentPosts.isEmpty()) {
                log.debug("No recent posts found within last 30 days");
                return new ArrayList<>();
            }

            // Calculate trending score for each post
            List<PostWithScore> postsWithScores = recentPosts.stream()
                    .map(post -> {
                        double score = calculateTrendingScore(post);
                        return new PostWithScore(post, score);
                    })
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .limit(limit)
                    .collect(Collectors.toList());

            // Convert to PostResponse
            return postsWithScores.stream()
                    .map(pws -> postService.mapToResponse(pws.post, currentUserId))
                    .filter(response -> response != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching trending posts", e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculate trending score for a post
     */
    private double calculateTrendingScore(Post post) {
        int likes = post.getLikesCount() != null ? post.getLikesCount() : 0;
        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;
        int views = post.getViewsCount() != null ? post.getViewsCount() : 0;

        // Base engagement score
        double engagementScore = (likes * 2.0) + (comments * 3.0) + (views * 0.1);

        // Recency bonus
        double recencyBonus = calculateRecencyBonus(post.getCreatedAt());

        // Media bonus - posts with media get slight boost
        double mediaBonus = post.getMediaCount() != null && post.getMediaCount() > 0 ? 20.0 : 0.0;

        return engagementScore + recencyBonus + mediaBonus;
    }

    /**
     * Calculate recency bonus for a post
     */
    private double calculateRecencyBonus(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        long hoursAgo = java.time.Duration.between(createdAt, now).toHours();

        if (hoursAgo < 24) {
            return 100.0; // Last 24 hours
        } else if (hoursAgo < 168) { // 7 days
            return 50.0;
        } else if (hoursAgo < 720) { // 30 days
            return 10.0;
        }
        return 0.0;
    }

    /**
     * Get popular users to follow
     * Algorithm: Score = (followers * 2) + (posts * 1) + (avg_post_engagement * 5)
     * - (following * 0.5)
     * Excludes users that the current user already follows
     */
    @Transactional(readOnly = true)
    public List<UserSuggestionResponse> getPopularUsers(String currentUserId, int limit) {
        log.debug("Fetching popular users for user: {} with limit: {}", currentUserId, limit);

        // Get users that current user is already following
        List<String> followingIds;
        if (currentUserId != null) {
            List<String> tempList = followRepository.findFollowingUserIds(currentUserId);
            tempList.add(currentUserId); // Exclude current user too
            followingIds = tempList;
        } else {
            followingIds = new ArrayList<>();
        }

        // Get all user profiles
        Pageable pageable = PageRequest.of(0, 100); // Get top 100 to calculate scores
        List<UserProfile> allProfiles = userProfileRepository.findAll(pageable).getContent();

        // Make final for lambda
        final List<String> finalFollowingIds = followingIds;

        // Filter out already following and calculate popularity scores
        List<UserWithScore> usersWithScores = allProfiles.stream()
                .filter(profile -> !finalFollowingIds.contains(profile.getUserId()))
                .map(profile -> {
                    double score = calculatePopularityScore(profile);
                    return new UserWithScore(profile, score);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .collect(Collectors.toList());

        // Convert to response
        return usersWithScores.stream()
                .map(uws -> convertToUserSuggestion(uws.profile, currentUserId, uws.score))
                .collect(Collectors.toList());
    }

    /**
     * Get smart user suggestions based on social network (mutual followers)
     * Logic: Suggest users followed by the people the current user is already
     * following.
     */
    @Transactional(readOnly = true)
    public List<SmartSuggestionResponse> getSmartUserSuggestions(String currentUserId, int limit) {
        log.debug("Fetching smart suggestions for user: {} with limit: {}", currentUserId, limit);

        if (currentUserId == null) {
            // Fallback to popular users if not logged in
            return getPopularUsers(null, limit).stream()
                    .map(pop -> SmartSuggestionResponse.builder()
                            .userId(pop.getUserId())
                            .userName(pop.getUserName())
                            .displayName(pop.getDisplayName())
                            .bio(pop.getBio())
                            .profilePictureUrl(pop.getProfilePictureUrl())
                            .followersCount(pop.getFollowersCount())
                            .followingCount(pop.getFollowingCount())
                            .postsCount(pop.getPostsCount())
                            .isFollowing(false)
                            .suggestionReason("Popular on platform")
                            .relevanceScore(pop.getPopularityScore())
                            .build())
                    .collect(Collectors.toList());
        }

        // 1. Get IDs of people the user follows AND people who follow the user
        List<String> userFollowingIds = followRepository.findFollowingUserIds(currentUserId);

        Pageable pageable = PageRequest.of(0, 100);
        List<String> userFollowerIds = followRepository.findByFollowingId(currentUserId, pageable)
                .map(Follow::getFollowerId)
                .getContent();

        // Combine seeds for suggestions
        java.util.Set<String> seedIds = new java.util.HashSet<>(userFollowingIds);
        seedIds.addAll(userFollowerIds);

        // Limit seeds to avoid performance issues
        List<String> limitedSeeds = seedIds.stream().limit(100).collect(Collectors.toList());

        java.util.Map<String, List<String>> suggestedUserToMutuals = new java.util.HashMap<>();

        for (String seedId : limitedSeeds) {
            List<String> followedBySeed = followRepository.findFollowingUserIds(seedId);
            for (String suggestedId : followedBySeed) {
                // Exclude current user and people already followed
                if (suggestedId.equals(currentUserId) || userFollowingIds.contains(suggestedId)) {
                    continue;
                }

                UserProfile seedProfile = userProfileRepository.findByUserId(seedId).orElse(null);
                String seedName = seedProfile != null ? seedProfile.getDisplayName() : seedId;

                suggestedUserToMutuals.computeIfAbsent(suggestedId, k -> new ArrayList<>()).add(seedName);
            }
        }

        // 3. Convert to response and sort
        return suggestedUserToMutuals.entrySet().stream()
                .map(entry -> {
                    String userId = entry.getKey();
                    List<String> mutualNames = entry.getValue();
                    UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

                    if (profile == null)
                        return null;

                    String reason;
                    if (mutualNames.size() == 1) {
                        reason = "Followed by " + mutualNames.get(0);
                    } else {
                        reason = "Followed by " + mutualNames.get(0) + " and " + (mutualNames.size() - 1)
                                + " others you know";
                    }

                    boolean isFollowing = currentUserId != null
                            && followRepository.existsByFollowerIdAndFollowingId(currentUserId, profile.getUserId());

                    return SmartSuggestionResponse.builder()
                            .userId(profile.getUserId())
                            .userName(profile.getUsername())
                            .displayName(profile.getDisplayName())
                            .bio(profile.getBio())
                            .profilePictureUrl(profile.getProfilePictureUrl())
                            .followersCount(followRepository.countByFollowingId(userId))
                            .followingCount(followRepository.countByFollowerId(userId))
                            .postsCount(postRepository.countByUserIdAndIsDeletedFalse(userId))
                            .isFollowing(isFollowing)
                            .mutualFollowersCount(mutualNames.size())
                            .mutualFollowerNames(mutualNames.stream().limit(3).collect(Collectors.toList()))
                            .suggestionReason(reason)
                            .relevanceScore((double) mutualNames.size())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculate popularity score for a user
     */
    private double calculatePopularityScore(UserProfile profile) {
        long followers = followRepository.countByFollowingId(profile.getUserId());
        long following = followRepository.countByFollowerId(profile.getUserId());
        long posts = postRepository.countByUserIdAndIsDeletedFalse(profile.getUserId());

        // Calculate average engagement per post
        double avgEngagement = 0.0;
        if (posts > 0) {
            Pageable pageable = PageRequest.of(0, 10); // Last 10 posts
            List<Post> recentPosts = postRepository.findByUserIdWithMedia(profile.getUserId(), pageable);

            int totalEngagement = recentPosts.stream()
                    .mapToInt(post -> {
                        int likes = post.getLikesCount() != null ? post.getLikesCount() : 0;
                        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;
                        return likes + comments;
                    })
                    .sum();

            avgEngagement = recentPosts.isEmpty() ? 0.0 : (double) totalEngagement / recentPosts.size();
        }

        // Popularity formula
        // Higher followers count = more popular
        // More posts = more active
        // Higher avg engagement = better content
        // Following many people reduces score slightly (spam accounts often follow
        // many)
        return (followers * 2.0) + (posts * 1.0) + (avgEngagement * 5.0) - (following * 0.5);
    }

    /**
     * Convert UserProfile to UserSuggestionResponse
     */
    private UserSuggestionResponse convertToUserSuggestion(UserProfile profile, String currentUserId, double score) {
        boolean isFollowing = false;
        if (currentUserId != null) {
            isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, profile.getUserId());
        }

        return UserSuggestionResponse.builder()
                .userId(profile.getUserId())
                .userName(profile.getUsername())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .followersCount(followRepository.countByFollowingId(profile.getUserId()))
                .followingCount(followRepository.countByFollowerId(profile.getUserId()))
                .postsCount(postRepository.countByUserIdAndIsDeletedFalse(profile.getUserId()))
                .isFollowing(isFollowing)
                .popularityScore(score)
                .build();
    }

    /**
     * Get platform statistics
     */
    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        log.debug("Fetching platform statistics");

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        // Total counts
        long totalUsers = userProfileRepository.count();
        long totalPosts = postRepository.count();
        long totalFollows = followRepository.count();

        // Aggregate metrics from posts
        List<Post> allPosts = postRepository.findAll();
        long totalLikes = allPosts.stream()
                .mapToLong(post -> post.getLikesCount() != null ? post.getLikesCount() : 0)
                .sum();
        long totalViews = allPosts.stream()
                .mapToLong(post -> post.getViewsCount() != null ? post.getViewsCount() : 0)
                .sum();
        long totalComments = allPosts.stream()
                .mapToLong(post -> post.getCommentsCount() != null ? post.getCommentsCount() : 0)
                .sum();

        // Today's stats
        long postsToday = allPosts.stream()
                .filter(post -> post.getCreatedAt().isAfter(today))
                .count();

        // Active users today (users who posted today)
        long activeUsersToday = allPosts.stream()
                .filter(post -> post.getCreatedAt().isAfter(today))
                .map(Post::getUserId)
                .distinct()
                .count();

        return PlatformStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalPosts(totalPosts)
                .totalLikes(totalLikes)
                .totalViews(totalViews)
                .totalComments(totalComments)
                .totalFollows(totalFollows)
                .activeUsersToday(activeUsersToday)
                .postsToday(postsToday)
                .build();
    }

    // Helper classes for sorting
    private static class PostWithScore {
        Post post;
        double score;

        PostWithScore(Post post, double score) {
            this.post = post;
            this.score = score;
        }
    }

    private static class UserWithScore {
        UserProfile profile;
        double score;

        UserWithScore(UserProfile profile, double score) {
            this.profile = profile;
            this.score = score;
        }
    }
}
