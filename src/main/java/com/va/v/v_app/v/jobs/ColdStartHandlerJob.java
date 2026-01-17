package com.va.v.v_app.v.jobs;

import com.va.v.v_app.config.feed.ForYouFeedConfig;
import com.va.v.v_app.v.repository.FollowRepository;
import com.va.v.v_app.v.repository.UserProfileRepository;
import com.va.v.v_app.v.service.ForYouFeedService;
import com.va.v.v_app.v.service.RedisForYouService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background job to handle cold start scenarios
 * Runs every 6 hours (configurable via cron expression)
 * 
 * Pre-generates feeds for new users who have no cached feed
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "feature.social.for-you.jobs.cold-start-handler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ColdStartHandlerJob {

    private final ForYouFeedConfig config;
    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;
    private final RedisForYouService redisService;
    private final ForYouFeedService forYouFeedService;

    /**
     * Pre-generate feeds for new users
     * Helps with cold start problem
     */
    @Scheduled(cron = "${feature.social.for-you.jobs.cold-start-handler.cron:0 0 */6 * * *}")
    public void handleColdStartUsers() {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting cold start handler job");

            // Find new users (created in last 7 days) with minimal following
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            int minFollowing = config.getEdgeCases().getMinFollowingForPersonalization();

            // Get all users created recently
            List<String> newUserIds = userProfileRepository.findRecentUsers(sevenDaysAgo)
                    .stream()
                    .map(profile -> profile.getUserId())
                    .toList();

            log.debug("Found {} recently created users", newUserIds.size());

            int processed = 0;
            for (String userId : newUserIds) {
                try {
                    // Check if user already has cached feed
                    List<Long> cachedFeed = redisService.getUserFeedPostIds(userId, 1);
                    if (cachedFeed != null && !cachedFeed.isEmpty()) {
                        continue; // Already has cache
                    }

                    // Check following count
                    long followingCount = followRepository.countByFollowerId(userId);

                    if (followingCount < minFollowing) {
                        // New user with minimal following - pre-generate feed
                        log.debug("Pre-generating feed for new user: {}", userId);
                        forYouFeedService.generateForYouFeed(userId, 0, 20);
                        processed++;
                    }

                } catch (Exception e) {
                    log.error("Error processing cold start for user: {}", userId, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cold start handler completed: {} feeds pre-generated in {}ms", processed, duration);

        } catch (Exception e) {
            log.error("Error in cold start handler job", e);
        }
    }
}
