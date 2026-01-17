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
import java.util.stream.Collectors;

/**
 * Background job to recalculate global trending scores
 * Runs every 5 minutes (configurable via cron expression)
 * 
 * Updates the Redis ZSET: trending:global:zset
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "feature.social.for-you.jobs.trending-calculator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TrendingCalculatorJob {

    private final ForYouFeedConfig config;
    private final PostRepository postRepository;
    private final RankingService rankingService;
    private final RedisForYouService redisService;

    /**
     * Recalculate trending scores for recent posts
     * Scheduled via cron expression from config
     */
    @Scheduled(cron = "${feature.social.for-you.jobs.trending-calculator.cron:0 */5 * * * *}")
    public void recalculateTrendingScores() {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting trending score recalculation job");

            // Get configuration
            int maxPosts = config.getPerformance().getMaxPostsToScore();
            int maxAgeDays = config.getRanking().getTimeDecay().getMaxAgeDays();
            int batchSize = config.getJobs().getTrendingCalculator().getBatchSize();

            // Fetch recent posts
            LocalDateTime since = LocalDateTime.now().minusDays(maxAgeDays);
            Pageable pageable = PageRequest.of(0, maxPosts);
            List<Post> recentPosts = postRepository.findRecentPosts(since, pageable);

            log.debug("Found {} posts for trending calculation", recentPosts.size());

            if (recentPosts.isEmpty()) {
                log.warn("No recent posts found for trending calculation");
                return;
            }

            // Clear existing trending data
            redisService.clearGlobalTrending();

            // Calculate scores and update Redis
            int processed = 0;
            for (Post post : recentPosts) {
                try {
                    // Skip posts below engagement threshold
                    if (!rankingService.meetsEngagementThreshold(post)) {
                        continue;
                    }

                    // Calculate trending score
                    ScoredPost scoredPost = rankingService.calculateTrendingScore(post);

                    // Add to Redis ZSET
                    redisService.addToGlobalTrending(scoredPost);

                    // Cache post score details
                    redisService.cachePostScore(scoredPost);

                    processed++;

                } catch (Exception e) {
                    log.error("Error processing post {} in trending calculation", post.getId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Trending calculation completed: {} posts processed in {}ms", processed, duration);

        } catch (Exception e) {
            log.error("Error in trending calculation job", e);
        }
    }

    /**
     * Cleanup old trending entries
     * Removes posts older than max age
     */
    @Scheduled(cron = "0  0 */6 * * *") // Every 6 hours
    public void cleanupOldTrendingPosts() {
        try {
            log.info("Starting trending cleanup job");

            int maxAgeDays = config.getRanking().getTimeDecay().getMaxAgeDays();
            long maxAgeMillis = maxAgeDays * 24 * 60 * 60 * 1000L;

            redisService.removeOldPosts(maxAgeMillis);

            log.info("Trending cleanup completed");

        } catch (Exception e) {
            log.error("Error in trending cleanup job", e);
        }
    }
}
