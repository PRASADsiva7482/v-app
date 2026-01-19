package com.va.v.v_app.v.jobs;

import com.va.v.v_app.config.feed.ForYouFeedConfig;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.model.ScoredPost;
import com.va.v.v_app.v.repository.PostRepository;
import com.va.v.v_app.v.service.RankingService;
import com.va.v.v_app.v.service.RedisForYouService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background job to incrementally update scores for recent posts
 * Runs every 2 minutes (configurable via cron expression)
 * 
 * Focuses on "hot" posts (last 24 hours) for faster updates
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "feature.social.for-you.jobs.score-updater", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScoreUpdaterJob {

    private final ForYouFeedConfig config;
    private final PostRepository postRepository;
    private final RankingService rankingService;
    private final RedisForYouService redisService;

    /**
     * Update scores for recent posts (last 24 hours)
     * These are the "hot" posts that change rapidly
     */
    @Scheduled(cron = "${feature.social.for-you.jobs.score-updater.cron:0 */2 * * * *}")
    public void updateRecentPostScores() {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Starting incremental score update job");

            // Get posts from last 24 hours (hot zone)
            LocalDateTime since = LocalDateTime.now().minusDays(1);
            int batchSize = config.getJobs().getScoreUpdater().getBatchSize();
            Pageable pageable = PageRequest.of(0, batchSize);

            List<Post> recentPosts = postRepository.findRecentPosts(since, pageable);

            if (recentPosts.isEmpty()) {
                log.debug("No recent posts to update");
                return;
            }

            log.debug("Updating scores for {} recent posts", recentPosts.size());

            int updated = 0;
            for (Post post : recentPosts) {
                try {
                    // Calculate fresh score
                    ScoredPost scoredPost = rankingService.calculateTrendingScore(post);

                    // Update Redis ZSET
                    redisService.addToGlobalTrending(scoredPost);

                    // Update cached score details
                    redisService.cachePostScore(scoredPost);

                    updated++;

                } catch (Exception e) {
                    log.error("Error updating score for post: {}", post.getId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Score update completed: {} posts updated in {}ms", updated, duration);

        } catch (Exception e) {
            log.error("Error in score update job", e);
        }
    }
}
