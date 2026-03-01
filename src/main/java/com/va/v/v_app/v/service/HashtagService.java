package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.HashtagResponse;
import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.model.Hashtag;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.model.PostHashtag;
import com.va.v.v_app.v.repository.HashtagRepository;
import com.va.v.v_app.v.repository.PostHashtagRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing hashtags
 */
@Service
@Slf4j
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostService postService;

    public HashtagService(
            HashtagRepository hashtagRepository,
            PostHashtagRepository postHashtagRepository,
            @Lazy PostService postService) {
        this.hashtagRepository = hashtagRepository;
        this.postHashtagRepository = postHashtagRepository;
        this.postService = postService;
    }

    // Regex pattern to extract hashtags from text
    // Matches #word (letters, numbers, underscores) but not standalone #
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([a-zA-Z0-9_]+)");

    /**
     * Extract hashtags from text content
     */
    public Set<String> extractHashtags(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> hashtags = new HashSet<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);

        while (matcher.find()) {
            String tag = matcher.group(1).toLowerCase(); // Extract without # and convert to lowercase
            if (tag.length() <= 100) { // Validate length
                hashtags.add(tag);
            }
        }

        return hashtags;
    }

    /**
     * Get or create hashtags from tag names
     */
    @Transactional
    public List<Hashtag> getOrCreateHashtags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<Hashtag> hashtags = new ArrayList<>();

        // Normalize tag names to lowercase
        Set<String> normalizedTags = tagNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Find existing hashtags
        List<Hashtag> existingHashtags = hashtagRepository.findByTagNameIn(new ArrayList<>(normalizedTags));
        Map<String, Hashtag> existingMap = existingHashtags.stream()
                .collect(Collectors.toMap(Hashtag::getTagName, h -> h));

        // Create new hashtags for tags that don't exist
        for (String tagName : normalizedTags) {
            if (existingMap.containsKey(tagName)) {
                hashtags.add(existingMap.get(tagName));
            } else {
                Hashtag newHashtag = Hashtag.builder()
                        .tagName(tagName)
                        .usageCount(0L)
                        .lastUsedAt(LocalDateTime.now())
                        .build();
                Hashtag saved = hashtagRepository.save(newHashtag);
                hashtags.add(saved);
                log.info("Created new hashtag: {}", tagName);
            }
        }

        return hashtags;
    }

    /**
     * Associate hashtags with a post
     */
    @Transactional
    public void associateHashtagsWithPost(Post post, String content) {
        if (post == null) {
            throw new IllegalArgumentException("Post cannot be null");
        }

        // Extract hashtags from content
        Set<String> tagNames = extractHashtags(content);

        if (tagNames.isEmpty()) {
            log.debug("No hashtags found in post ID: {}", post.getId());
            return;
        }

        // Get or create hashtags
        List<Hashtag> hashtags = getOrCreateHashtags(tagNames);

        // Create associations
        LocalDateTime now = LocalDateTime.now();
        for (Hashtag hashtag : hashtags) {
            // Check if association already exists
            if (!postHashtagRepository.existsByPostIdAndHashtagId(post.getId(), hashtag.getId())) {
                PostHashtag postHashtag = PostHashtag.builder()
                        .post(post)
                        .hashtag(hashtag)
                        .build();
                postHashtagRepository.save(postHashtag);

                // Increment usage count
                hashtagRepository.incrementUsageCount(hashtag.getId(), now);

                log.debug("Associated hashtag '{}' with post ID: {}", hashtag.getTagName(), post.getId());
            }
        }

        log.info("Associated {} hashtags with post ID: {}", hashtags.size(), post.getId());
    }

    /**
     * Remove all hashtag associations for a post
     */
    @Transactional
    public void removeHashtagsFromPost(Long postId) {
        List<PostHashtag> postHashtags = postHashtagRepository.findByPostId(postId);

        for (PostHashtag postHashtag : postHashtags) {
            hashtagRepository.decrementUsageCount(postHashtag.getHashtag().getId());
        }

        postHashtagRepository.deleteByPostId(postId);
        log.info("Removed all hashtag associations for post ID: {}", postId);
    }

    /**
     * Get hashtags for a specific post
     */
    @Transactional(readOnly = true)
    public List<HashtagResponse> getHashtagsForPost(Long postId) {
        List<Hashtag> hashtags = postHashtagRepository.findHashtagsByPostId(postId);
        return hashtags.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get posts by hashtag name
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByHashtag(String tagName, String currentUserId, Pageable pageable) {
        String normalizedTag = tagName.toLowerCase();
        if (normalizedTag.startsWith("#")) {
            normalizedTag = normalizedTag.substring(1);
        }

        Page<Post> posts = postHashtagRepository.findPostsByHashtagName(normalizedTag, pageable);
        return posts.map(post -> postService.mapToResponse(post, currentUserId));
    }

    /**
     * Search hashtags by prefix
     */
    @Transactional(readOnly = true)
    public Page<HashtagResponse> searchHashtags(String query, Pageable pageable) {
        String normalizedQuery = query.toLowerCase();
        if (normalizedQuery.startsWith("#")) {
            normalizedQuery = normalizedQuery.substring(1);
        }

        Page<Hashtag> hashtags = hashtagRepository.searchHashtagsByPrefix(normalizedQuery, pageable);
        return hashtags.map(this::mapToResponse);
    }

    /**
     * Get trending hashtags (used in last 7 days)
     */
    @Transactional(readOnly = true)
    public Page<HashtagResponse> getTrendingHashtags(Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Page<Hashtag> hashtags = hashtagRepository.findTrendingHashtags(since, pageable);
        return hashtags.map(this::mapToResponse);
    }

    /**
     * Get top hashtags by usage count
     */
    @Transactional(readOnly = true)
    public Page<HashtagResponse> getTopHashtags(Pageable pageable) {
        Page<Hashtag> hashtags = hashtagRepository.findAllByOrderByUsageCountDesc(pageable);
        return hashtags.map(this::mapToResponse);
    }

    /**
     * Get hashtag by name
     */
    @Transactional(readOnly = true)
    public HashtagResponse getHashtagByName(String tagName) {
        String normalizedTag = tagName.toLowerCase();
        if (normalizedTag.startsWith("#")) {
            normalizedTag = normalizedTag.substring(1);
        }

        Hashtag hashtag = hashtagRepository.findByTagName(normalizedTag)
                .orElseThrow(() -> new RuntimeException("Hashtag not found: " + tagName));
        return mapToResponse(hashtag);
    }

    /**
     * Map entity to response DTO
     */
    private HashtagResponse mapToResponse(Hashtag hashtag) {
        return HashtagResponse.builder()
                .id(hashtag.getId())
                .tagName(hashtag.getTagName())
                .usageCount(hashtag.getUsageCount())
                .createdAt(hashtag.getCreatedAt())
                .lastUsedAt(hashtag.getLastUsedAt())
                .build();
    }
}
