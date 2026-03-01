package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.*;
import com.va.v.v_app.v.model.*;
import com.va.v.v_app.v.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Explore page features: trending topics, news, categories
 * Aggregates data from explore tables + existing hashtag and post data
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExploreService {

    private final ExploreCategoryRepository categoryRepository;
    private final ExploreTopicRepository topicRepository;
    private final ExploreNewsRepository newsRepository;
    private final HashtagRepository hashtagRepository;
    private final HashtagService hashtagService;
    private final DiscoveryService discoveryService;

    /**
     * Get the full "For You" explore page data
     * Combines trending topics, news, hashtags, and trending posts
     */
    public ExplorePageResponse getForYouPage(String currentUserId, int topicLimit, int newsLimit) {
        log.info("Building Explore 'For You' page for user: {}", currentUserId);

        // Fetch categories
        List<ExploreCategoryResponse> categories = getCategories();

        // Fetch trending topics (mix from DB + auto-generated from hashtags)
        List<ExploreTopicResponse> trendingTopics = getTrendingTopics(topicLimit);

        // Fetch news
        List<ExploreNewsResponse> news = getNews(null, newsLimit);

        // Fetch trending hashtags
        Pageable hashtagPageable = PageRequest.of(0, 10);
        List<HashtagResponse> trendingHashtags = hashtagService
                .getTrendingHashtags(hashtagPageable)
                .getContent();

        // Fetch trending posts
        List<PostResponse> trendingPosts = discoveryService.getTrendingPosts(currentUserId, 10);

        return ExplorePageResponse.builder()
                .categories(categories)
                .trendingTopics(trendingTopics)
                .news(news)
                .trendingHashtags(trendingHashtags)
                .trendingPosts(trendingPosts)
                .build();
    }

    /**
     * Get all active categories
     */
    public List<ExploreCategoryResponse> getCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::mapCategoryToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get trending topics, combining curated + auto-generated from popular hashtags
     */
    public List<ExploreTopicResponse> getTrendingTopics(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ExploreTopicResponse> topics = new ArrayList<>();

        // 1. Get curated topics from explore_topic table
        List<ExploreTopic> curatedTopics = topicRepository.findActiveTrendingTopics(pageable);
        topics.addAll(curatedTopics.stream()
                .map(this::mapTopicToResponse)
                .collect(Collectors.toList()));

        // 2. If we don't have enough curated topics, supplement with auto-generated
        // from hashtags
        if (topics.size() < limit) {
            int remaining = limit - topics.size();
            Pageable hashtagPageable = PageRequest.of(0, remaining);
            List<Hashtag> trendingHashtags = hashtagRepository
                    .findTrendingHashtags(LocalDateTime.now().minusDays(7), hashtagPageable)
                    .getContent();

            for (Hashtag hashtag : trendingHashtags) {
                // Skip if already included as a curated topic
                boolean alreadyIncluded = topics.stream()
                        .anyMatch(t -> Boolean.TRUE.equals(t.getIsHashtag())
                                && hashtag.getTagName().equals(t.getHashtagName()));
                if (!alreadyIncluded) {
                    topics.add(ExploreTopicResponse.builder()
                            .id(hashtag.getId())
                            .title("#" + hashtag.getTagName())
                            .categoryName("trending")
                            .categoryDisplayName("Trending")
                            .categoryIcon("🔥")
                            .postCount(hashtag.getUsageCount())
                            .isHashtag(true)
                            .hashtagName(hashtag.getTagName())
                            .isPromoted(false)
                            .trendingScore(hashtag.getUsageCount() != null ? hashtag.getUsageCount().doubleValue() : 0)
                            .startedTrendingAt(hashtag.getLastUsedAt())
                            .createdAt(hashtag.getCreatedAt())
                            .build());
                }
            }
        }

        // Limit to requested size
        if (topics.size() > limit) {
            topics = topics.subList(0, limit);
        }

        return topics;
    }

    /**
     * Get trending topics by category
     */
    public List<ExploreTopicResponse> getTopicsByCategory(String categoryName, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ExploreTopicResponse> topics = new ArrayList<>();

        // Get curated topics for this category
        List<ExploreTopic> curatedTopics = topicRepository
                .findActiveTrendingTopicsByCategory(categoryName, pageable);
        topics.addAll(curatedTopics.stream()
                .map(this::mapTopicToResponse)
                .collect(Collectors.toList()));

        // If not enough, supplement with trending hashtags flagged as this category
        if (topics.size() < limit && "trending".equals(categoryName)) {
            int remaining = limit - topics.size();
            Pageable hashtagPageable = PageRequest.of(0, remaining);
            List<Hashtag> trendingHashtags = hashtagRepository
                    .findTrendingHashtags(LocalDateTime.now().minusDays(7), hashtagPageable)
                    .getContent();

            for (Hashtag hashtag : trendingHashtags) {
                boolean alreadyIncluded = topics.stream()
                        .anyMatch(t -> Boolean.TRUE.equals(t.getIsHashtag())
                                && hashtag.getTagName().equals(t.getHashtagName()));
                if (!alreadyIncluded) {
                    topics.add(ExploreTopicResponse.builder()
                            .id(hashtag.getId())
                            .title("#" + hashtag.getTagName())
                            .categoryName("trending")
                            .categoryDisplayName("Trending")
                            .categoryIcon("🔥")
                            .postCount(hashtag.getUsageCount())
                            .isHashtag(true)
                            .hashtagName(hashtag.getTagName())
                            .isPromoted(false)
                            .trendingScore(hashtag.getUsageCount() != null ? hashtag.getUsageCount().doubleValue() : 0)
                            .createdAt(hashtag.getCreatedAt())
                            .build());
                }
            }
        }

        if (topics.size() > limit) {
            topics = topics.subList(0, limit);
        }

        return topics;
    }

    /**
     * Get news items, optionally filtered by category
     */
    public List<ExploreNewsResponse> getNews(String categoryName, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ExploreNews> newsList;

        if (categoryName != null && !categoryName.isEmpty()) {
            newsList = newsRepository.findActiveNewsByCategory(categoryName, pageable);
        } else {
            newsList = newsRepository.findActiveNews(pageable);
        }

        return newsList.stream()
                .map(this::mapNewsToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get breaking news
     */
    public List<ExploreNewsResponse> getBreakingNews(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return newsRepository.findBreakingNews(pageable)
                .stream()
                .map(this::mapNewsToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search explore topics
     */
    public List<ExploreTopicResponse> searchTopics(String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return topicRepository.searchTopics(query, pageable)
                .stream()
                .map(this::mapTopicToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get trending posts by category (uses post search with category-related
     * keywords)
     */
    public List<PostResponse> getTrendingPostsByCategory(String categoryName, String currentUserId, int limit) {
        // For now, we use the trending posts from discovery service
        // In the future, posts could be tagged with categories
        return discoveryService.getTrendingPosts(currentUserId, limit);
    }

    // ========== Mapping Methods ==========

    private ExploreCategoryResponse mapCategoryToResponse(ExploreCategory category) {
        return ExploreCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .displayName(category.getDisplayName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .build();
    }

    private ExploreTopicResponse mapTopicToResponse(ExploreTopic topic) {
        ExploreTopicResponse.ExploreTopicResponseBuilder builder = ExploreTopicResponse.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .description(topic.getDescription())
                .location(topic.getLocation())
                .postCount(topic.getPostCount())
                .isHashtag(topic.getIsHashtag())
                .isPromoted(topic.getIsPromoted())
                .trendingScore(topic.getTrendingScore())
                .startedTrendingAt(topic.getStartedTrendingAt())
                .createdAt(topic.getCreatedAt());

        if (topic.getCategory() != null) {
            builder.categoryName(topic.getCategory().getName())
                    .categoryDisplayName(topic.getCategory().getDisplayName())
                    .categoryIcon(topic.getCategory().getIcon());
        }

        if (topic.getHashtag() != null) {
            builder.hashtagName(topic.getHashtag().getTagName());
        }

        return builder.build();
    }

    private ExploreNewsResponse mapNewsToResponse(ExploreNews news) {
        ExploreNewsResponse.ExploreNewsResponseBuilder builder = ExploreNewsResponse.builder()
                .id(news.getId())
                .headline(news.getHeadline())
                .description(news.getDescription())
                .source(news.getSource())
                .sourceUrl(news.getSourceUrl())
                .imageUrl(news.getImageUrl())
                .postCount(news.getPostCount())
                .isBreaking(news.getIsBreaking())
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt());

        if (news.getCategory() != null) {
            builder.categoryName(news.getCategory().getName())
                    .categoryDisplayName(news.getCategory().getDisplayName())
                    .categoryIcon(news.getCategory().getIcon());
        }

        return builder.build();
    }
}
