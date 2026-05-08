package com.va.v.v_app.social.service;

import com.va.v.v_app.social.exception.ResourceNotFoundException;
import com.va.v.v_app.social.model.Story;
import com.va.v.v_app.social.repository.FollowRepository;
import com.va.v.v_app.social.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {

    private final StoryRepository storyRepository;
    private final FollowRepository followRepository;

    @Transactional
    public Map<String, Object> createStory(String userId, String mediaUrl, String mediaType, String caption) {
        Story story = Story.builder()
                .userId(userId)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType != null ? mediaType : "IMAGE")
                .caption(caption)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        Story saved = storyRepository.save(story);
        return mapStory(saved);
    }

    public List<Map<String, Object>> getMyStories(String userId) {
        return storyRepository.findByUserIdAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(userId, LocalDateTime.now())
                .stream().map(this::mapStory).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFeedStories(String userId) {
        // Get list of users the current user follows
        List<String> followingIds = followRepository.findFollowingUserIds(userId);
        followingIds.add(userId); // Include own stories
        return storyRepository.findByUserIdInAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(followingIds, LocalDateTime.now())
                .stream().map(this::mapStory).collect(Collectors.toList());
    }

    @Transactional
    public void viewStory(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        story.setViewCount(story.getViewCount() + 1);
        storyRepository.save(story);
    }

    @Transactional
    public void deleteStory(Long storyId, String userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");
        story.setIsActive(false);
        storyRepository.save(story);
    }

    private Map<String, Object> mapStory(Story s) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.getId());
        map.put("userId", s.getUserId());
        map.put("mediaUrl", s.getMediaUrl());
        map.put("mediaType", s.getMediaType());
        map.put("caption", s.getCaption());
        map.put("viewCount", s.getViewCount());
        map.put("createdAt", s.getCreatedAt());
        map.put("expiresAt", s.getExpiresAt());
        return map;
    }
}
