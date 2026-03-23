package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.repository.PostRepository;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.config.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationFeedController {

    private final PostRepository postRepository;

    @GetMapping("/nearby")
    public ResponseEntity<List<Map<String, Object>>> getNearbyPosts(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(defaultValue = "20") int limit) {
        List<Long> postIds = postRepository.findNearbyPostIds(lat, lng, radiusKm, limit);
        List<Post> posts = postRepository.findByIdInAndIsDeletedFalse(postIds);

        List<Map<String, Object>> result = posts.stream().map(post -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("userId", post.getUserId());
            map.put("content", post.getContent());
            map.put("latitude", post.getLatitude());
            map.put("longitude", post.getLongitude());
            map.put("locationName", post.getLocationName());
            map.put("likesCount", post.getLikesCount());
            map.put("commentsCount", post.getCommentsCount());
            map.put("createdAt", post.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
