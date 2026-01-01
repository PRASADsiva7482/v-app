package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.*;
import com.va.v.v_app.v.model.Hashtag;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.model.TrendingTopic;
import com.va.v.v_app.v.repository.HashtagRepository;
import com.va.v.v_app.v.repository.PostRepository;
import com.va.v.v_app.v.repository.PostHashtagRepository;
import com.va.v.v_app.v.repository.TrendingTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Explore feature - aggregates trending content across categories
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ExploreService {

    private final TrendingTopicRepository trendingTopicRepository;
    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final DiscoveryService discoveryService;
    private final HashtagService hashtagService;

    /**
     * Get explore content for a specific category
     * Note: No @Transactional here as this method aggregates data from multiple
     * transactional services. Each service manages its own transaction.
     */
    public ExploreContentResponse getExploreContent(String category, String currentUserId, int limit) {
        log.debug("Fetching explore content for category: {}", category);

        ExploreContentResponse.ExploreContentResponseBuilder builder = ExploreContentResponse.builder()
                .category(category);

        try {
            // Get trending topics for the category
            Pageable pageable = PageRequest.of(0, limit);
            Page<TrendingTopic> trendingTopicsPage;

            if ("FOR_YOU".equals(category)) {
                // For "For You", mix content from all categories
                trendingTopicsPage = trendingTopicRepository.findByIsActiveTrueOrderByTrendScoreDesc(pageable);
            } else {
                trendingTopicsPage = trendingTopicRepository
                        .findByIsActiveTrueAndCategoryOrderByTrendScoreDesc(category, pageable);
            }

            List<TrendingTopicResponse> trendingTopics = trendingTopicsPage.getContent().stream()
                    .map(this::mapToTrendingTopicResponse)
                    .collect(Collectors.toList());

            builder.trendingTopics(trendingTopics);
        } catch (Exception e) {
            log.error("Error fetching trending topics", e);
            builder.trendingTopics(new ArrayList<>());
        }

        try {
            // Get trending hashtags
            Page<HashtagResponse> hashtagsPage = hashtagService.getTrendingHashtags(PageRequest.of(0, 10));
            builder.trendingHashtags(hashtagsPage.getContent());
        } catch (Exception e) {
            log.error("Error fetching trending hashtags", e);
            builder.trendingHashtags(new ArrayList<>());
        }

        try {
            // Get trending posts
            List<PostResponse> trendingPosts = discoveryService.getTrendingPosts(currentUserId, limit);
            builder.trendingPosts(trendingPosts);
        } catch (Exception e) {
            log.error("Error fetching trending posts", e);
            builder.trendingPosts(new ArrayList<>());
        }

        // Get suggested users (only for FOR_YOU category)
        if ("FOR_YOU".equals(category)) {
            try {
                List<UserSuggestionResponse> suggestedUsers = discoveryService.getPopularUsers(currentUserId, 5);
                builder.suggestedUsers(suggestedUsers);
            } catch (Exception e) {
                log.error("Error fetching suggested users", e);
                builder.suggestedUsers(new ArrayList<>());
            }
        }

        return builder.build();
    }

    /**
     * Get all trending topics
     */
    @Transactional(readOnly = true)
    public Page<TrendingTopicResponse> getTrendingTopics(String category, int page, int size) {
        log.debug("Fetching trending topics for category: {}, page: {}, size: {}", category, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<TrendingTopic> topics;

        if (category == null || category.isEmpty() || "ALL".equals(category)) {
            topics = trendingTopicRepository.findByIsActiveTrueOrderByTrendScoreDesc(pageable);
        } else {
            topics = trendingTopicRepository
                    .findByIsActiveTrueAndCategoryOrderByTrendScoreDesc(category, pageable);
        }

        return topics.map(this::mapToTrendingTopicResponse);
    }

    /**
     * Update trending scores - runs periodically
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void updateTrendingScores() {
        log.debug("Starting scheduled trending score update");

        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // Get top hashtags from last 7 days
            Page<Hashtag> topHashtags = hashtagRepository.findTopHashtagsByLastUsed(
                    sevenDaysAgo, PageRequest.of(0, 50));

            for (Hashtag hashtag : topHashtags) {
                updateOrCreateTrendingTopic(hashtag);
            }

            // Deactivate old trending topics (older than 7 days without updates)
            LocalDateTime deactivateThreshold = LocalDateTime.now().minusDays(7);
            List<TrendingTopic> oldTopics = trendingTopicRepository
                    .findRecentTrending(deactivateThreshold, PageRequest.of(0, 1000));

            for (TrendingTopic topic : oldTopics) {
                if (topic.getUpdatedAt().isBefore(deactivateThreshold)) {
                    topic.setIsActive(false);
                    trendingTopicRepository.save(topic);
                }
            }

            log.debug("Completed trending score update");
        } catch (Exception e) {
            log.error("Error updating trending scores", e);
        }
    }

    /**
     * Update or create trending topic from hashtag
     */
    private void updateOrCreateTrendingTopic(Hashtag hashtag) {
        TrendingTopic existingTopic = trendingTopicRepository.findByHashtagIdAndIsActiveTrue(hashtag.getId());

        // Calculate trend score
        double trendScore = calculateHashtagTrendScore(hashtag);

        // Determine category based on content analysis (simplified version)
        String category = categorizeHashtag(hashtag.getTagName());

        if (existingTopic != null) {
            // Update existing topic
            existingTopic.setPostCount(hashtag.getUsageCount());
            existingTopic.setTrendScore(trendScore);
            existingTopic.setCategory(category);
            trendingTopicRepository.save(existingTopic);
        } else {
            // Create new trending topic
            TrendingTopic newTopic = TrendingTopic.builder()
                    .title("#" + hashtag.getTagName())
                    .description(hashtag.getUsageCount() + " posts")
                    .category(category)
                    .hashtagId(hashtag.getId())
                    .postCount(hashtag.getUsageCount())
                    .trendScore(trendScore)
                    .region("Global")
                    .isActive(true)
                    .trendingSince(hashtag.getLastUsedAt())
                    .build();

            trendingTopicRepository.save(newTopic);
        }
    }

    /**
     * Calculate trend score for a hashtag
     */
    private double calculateHashtagTrendScore(Hashtag hashtag) {
        long usageCount = hashtag.getUsageCount() != null ? hashtag.getUsageCount() : 0;

        // Recency bonus
        double recencyBonus = 0.0;
        if (hashtag.getLastUsedAt() != null) {
            long hoursAgo = java.time.Duration.between(hashtag.getLastUsedAt(), LocalDateTime.now()).toHours();
            if (hoursAgo < 24) {
                recencyBonus = 100.0;
            } else if (hoursAgo < 168) { // 7 days
                recencyBonus = 50.0;
            }
        }

        // Get engagement from recent posts with this hashtag
        double engagementScore = getHashtagEngagementScore(hashtag.getId());

        return (usageCount * 2.0) + recencyBonus + (engagementScore * 1.5);
    }

    /**
     * Get engagement score for posts with a specific hashtag
     */
    private double getHashtagEngagementScore(Long hashtagId) {
        try {
            // Use repository query to get posts with this hashtag (avoids lazy loading
            // issues)
            Page<Post> postsPage = postHashtagRepository.findPostsByHashtagId(
                    hashtagId, PageRequest.of(0, 10));

            List<Post> posts = postsPage.getContent();

            if (posts.isEmpty()) {
                return 0.0;
            }

            double totalEngagement = posts.stream()
                    .mapToDouble(post -> {
                        int likes = post.getLikesCount() != null ? post.getLikesCount() : 0;
                        int comments = post.getCommentsCount() != null ? post.getCommentsCount() : 0;
                        int views = post.getViewsCount() != null ? post.getViewsCount() : 0;
                        return (likes * 2.0) + (comments * 3.0) + (views * 0.1);
                    })
                    .sum();

            return totalEngagement / posts.size();
        } catch (Exception e) {
            log.warn("Error calculating engagement score for hashtag: {}", hashtagId, e);
            return 0.0;
        }
    }

    /**
     * Categorize hashtag into a category (simplified)
     */
    private String categorizeHashtag(String tagName) {
        String lowerTag = tagName.toLowerCase();

        // Sports keywords
        if (lowerTag.matches(".*(cricket|football|soccer|tennis|basketball|sports|game|match|player|team).*")) {
            return "SPORTS";
        }

        // News keywords
        if (lowerTag.matches(".*(news|breaking|update|alert|policy|government|politics|election).*")) {
            return "NEWS";
        }

        // Entertainment keywords
        if (lowerTag.matches(".*(movie|film|music|celebrity|actor|singer|entertainment|show|series|tv).*")) {
            return "ENTERTAINMENT";
        }

        // Default to TRENDING
        return "TRENDING";
    }

    /**
     * Map TrendingTopic entity to response DTO
     */
    private TrendingTopicResponse mapToTrendingTopicResponse(TrendingTopic topic) {
        TrendingTopicResponse.TrendingTopicResponseBuilder builder = TrendingTopicResponse.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .description(topic.getDescription())
                .category(topic.getCategory())
                .postCount(topic.getPostCount())
                .trendScore(topic.getTrendScore())
                .region(topic.getRegion())
                .imageUrl(topic.getImageUrl())
                .trendingSince(topic.getTrendingSince())
                .createdAt(topic.getCreatedAt());

        // If linked to hashtag, add hashtag name
        if (topic.getHashtagId() != null) {
            hashtagRepository.findById(topic.getHashtagId())
                    .ifPresent(hashtag -> builder.hashtagName(hashtag.getTagName()));
        }

        return builder.build();
    }
}
