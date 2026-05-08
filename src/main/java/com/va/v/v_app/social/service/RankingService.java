package com.va.v.v_app.social.service;

import com.va.v.v_app.social.config.ForYouFeedConfig;
import com.va.v.v_app.social.model.Post;
import com.va.v.v_app.social.model.ScoredPost;
import com.va.v.v_app.social.repository.FollowRepository;
import com.va.v.v_app.social.repository.PostHashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service for calculating ranking scores for posts
 * Implements the enterprise ranking algorithm:
 * 
 * FINAL_SCORE = ENGAGEMENT_SCORE × TIME_DECAY + RECENCY_BONUS +
 * PERSONALIZATION_BOOST
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final ForYouFeedConfig config;
    private final FollowRepository followRepository;
    private final PostHashtagRepository postHashtagRepository;

    /**
     * Calculate trending score for a post (global, no personalization)
     */
    public ScoredPost calculateTrendingScore(Post post) {
        ScoredPost scoredPost = convertToScoredPost(post);

        // 1. Calculate base engagement score
        double engagementScore = calculateEngagementScore(post);
        scoredPost.setEngagementScore(engagementScore);

        // 2. Calculate engagement velocity
        double velocity = calculateEngagementVelocity(post);
        scoredPost.setEngagementVelocity(velocity);

        // 3. Apply time decay
        double timeDecayFactor = calculateTimeDecay(post.getCreatedAt());
        scoredPost.setTimeDecayFactor(timeDecayFactor);

        // 4. Calculate recency bonus
        double recencyBonus = calculateRecencyBonus(post.getCreatedAt());
        scoredPost.setRecencyBonus(recencyBonus);

        // 5. Calculate media bonus
        double mediaBonus = calculateMediaBonus(post);
        scoredPost.setMediaBonus(mediaBonus);

        // 6. Final trending score
        double trendingScore = (engagementScore + (velocity * config.getRanking().getWeights().getEngagementVelocity()))
                * timeDecayFactor
                + recencyBonus
                + mediaBonus;

        scoredPost.setTrendingScore(trendingScore);
        scoredPost.setPersonalizedScore(trendingScore); // Same as trending for non-personalized
        scoredPost.setScoreCalculatedAt(LocalDateTime.now());
        scoredPost.setScoreVersion("v1.0");

        return scoredPost;
    }

    /**
     * Calculate personalized score for a specific user
     */
    public ScoredPost calculatePersonalizedScore(Post post, String userId, Set<String> userInterests) {
        // Start with trending score
        ScoredPost scoredPost = calculateTrendingScore(post);

        if (!config.getPersonalization().isEnabled() || userId == null) {
            return scoredPost;
        }

        double trendingScore = scoredPost.getTrendingScore();
        double personalizationBoost = 0.0;

        // 1. User affinity boost (following, mutual, interaction)
        double affinityBoost = calculateAffinityBoost(post.getUserId(), userId, trendingScore);
        personalizationBoost += affinityBoost;

        // 2. Interest matching boost
        if (config.getPersonalization().getInterestMatching().isEnabled() && userInterests != null) {
            double interestBoost = calculateInterestBoost(post, userInterests);
            personalizationBoost += interestBoost;
        }

        // 3. Apply personalization
        double personalizedScore = trendingScore + personalizationBoost;

        scoredPost.setPersonalizationBoost(personalizationBoost);
        scoredPost.setPersonalizedScore(personalizedScore);

        return scoredPost;
    }

    /**
     * Calculate base engagement score
     * Formula: (likes × 3) + (comments × 5) + (shares × 7) + (views × 0.1)
     */
    private double calculateEngagementScore(Post post) {
        var weights = config.getRanking().getWeights();

        int likes = post.getLikesCount() != null ? post.getLikesCount() : 0;
        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;
        int shares = post.getRepostCount() != null ? post.getRepostCount() : 0;
        int views = post.getViewsCount() != null ? post.getViewsCount() : 0;

        return (likes * weights.getLikes()) +
                (comments * weights.getComments()) +
                (shares * weights.getShares()) +
                (views * weights.getViews());
    }

    /**
     * Calculate engagement velocity (engagement rate over time)
     * Formula: total_engagement / age_hours
     */
    private double calculateEngagementVelocity(Post post) {
        long ageHours = Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours();
        if (ageHours == 0) {
            ageHours = 1; // Prevent division by zero, use 1 hour minimum
        }

        int likes = post.getLikesCount() != null ? post.getLikesCount() : 0;
        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;
        int shares = post.getRepostCount() != null ? post.getRepostCount() : 0;

        int totalEngagement = likes + comments + (shares * 2); // Shares count double

        return (double) totalEngagement / ageHours;
    }

    /**
     * Calculate time decay factor using exponential decay
     * Formula: e^(-λ × age_hours)
     * where λ = ln(2) / half_life_hours
     */
    private double calculateTimeDecay(LocalDateTime createdAt) {
        if (!config.getRanking().getTimeDecay().isEnabled()) {
            return 1.0; // No decay
        }

        long ageHours = Duration.between(createdAt, LocalDateTime.now()).toHours();
        int halfLifeHours = config.getRanking().getTimeDecay().getHalfLifeHours();

        // Calculate decay constant λ
        double lambda = Math.log(2.0) / halfLifeHours;

        // Apply exponential decay: e^(-λt)
        return Math.exp(-lambda * ageHours);
    }

    /**
     * Calculate recency bonus (additive boost for fresh content)
     */
    private double calculateRecencyBonus(LocalDateTime createdAt) {
        long ageHours = Duration.between(createdAt, LocalDateTime.now()).toHours();
        var bonusConfig = config.getRanking().getRecencyBonus();

        if (ageHours < 1) {
            return bonusConfig.getLastHour();
        } else if (ageHours < 6) {
            return bonusConfig.getLast6Hours();
        } else if (ageHours < 24) {
            return bonusConfig.getLast24Hours();
        } else if (ageHours < 168) { // 7 days
            return bonusConfig.getLast7Days();
        } else if (ageHours < 720) { // 30 days
            return bonusConfig.getLast30Days();
        }

        return 0.0;
    }

    /**
     * Calculate media bonus
     */
    private double calculateMediaBonus(Post post) {
        Integer mediaCount = post.getMediaCount();
        if (mediaCount == null || mediaCount == 0) {
            return 0.0;
        }

        var bonusConfig = config.getRanking().getMediaBonus();

        if (mediaCount > 1) {
            return bonusConfig.getHasMultiple();
        } else {
            // Assume image if only one media (can be enhanced with actual media type check)
            return bonusConfig.getHasImage();
        }
    }

    /**
     * Calculate affinity boost based on user relationship
     */
    private double calculateAffinityBoost(String authorId, String userId, double baseScore) {
        var affinityConfig = config.getPersonalization().getAffinity();

        // Check if user follows author
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(userId, authorId);
        if (!isFollowing) {
            return 0.0;
        }

        // Check if mutual follow
        boolean isMutual = followRepository.existsByFollowerIdAndFollowingId(authorId, userId);

        if (isMutual) {
            // Mutual follow: 2x boost
            return baseScore * (affinityConfig.getMutualBoost() - 1.0);
        } else {
            // Regular follow: 1.5x boost
            return baseScore * (affinityConfig.getFollowingBoost() - 1.0);
        }
    }

    /**
     * Calculate interest matching boost
     */
    private double calculateInterestBoost(Post post, Set<String> userInterests) {
        if (userInterests == null || userInterests.isEmpty()) {
            return 0.0;
        }

        // Get post hashtags
        List<String> postHashtags = postHashtagRepository.findHashtagsByPostId(post.getId())
                .stream()
                .map(hashtag -> "hashtag:" + hashtag.getTagName())
                .toList();

        if (postHashtags.isEmpty()) {
            return 0.0;
        }

        // Count matching interests
        long matchCount = postHashtags.stream()
                .filter(userInterests::contains)
                .count();

        if (matchCount == 0) {
            return 0.0;
        }

        // Each matching hashtag gives a boost
        double hashtagBoost = config.getPersonalization().getInterestMatching().getHashtagMatchBoost();
        return matchCount * hashtagBoost;
    }

    /**
     * Convert Post entity to ScoredPost
     */
    private ScoredPost convertToScoredPost(Post post) {
        return ScoredPost.builder()
                .postId(post.getId())
                .userId(post.getUserId())
                .createdAt(post.getCreatedAt())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getRepostCount())
                .viewsCount(post.getViewsCount())
                .mediaCount(post.getMediaCount())
                .build();
    }

    /**
     * Check if post meets minimum engagement threshold
     */
    public boolean meetsEngagementThreshold(Post post) {
        int threshold = config.getPerformance().getMinEngagementThreshold();
        int likes = post.getLikesCount() != null ? post.getLikesCount() : 0;
        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;

        return (likes + comments) >= threshold;
    }
}
