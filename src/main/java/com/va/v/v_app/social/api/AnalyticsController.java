package com.va.v.v_app.social.api;

import com.va.v.v_app.social.repository.PostRepository;
import com.va.v.v_app.social.repository.FollowRepository;
import com.va.v.v_app.social.repository.PostLikeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "User analytics & insights")
public class AnalyticsController {

    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final PostLikeRepository postLikeRepository;

    @Operation(summary = "Get user analytics dashboard data")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication authentication) {
        String userId = authentication.getName();

        long totalPosts = postRepository.countByUserIdAndIsDeletedFalse(userId);
        long totalLikes = postLikeRepository.countLikesByPostOwner(userId);
        long followers = followRepository.countByFollowingId(userId);
        long following = followRepository.countByFollowerId(userId);

        return ResponseEntity.ok(Map.of(
                "totalPosts", totalPosts,
                "totalLikes", totalLikes,
                "followers", followers,
                "following", following,
                "engagementRate", totalPosts > 0 ? (double) totalLikes / totalPosts : 0.0
        ));
    }
}
