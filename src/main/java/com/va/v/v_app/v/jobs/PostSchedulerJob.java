package com.va.v.v_app.v.jobs;

import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.PostRepository;
import com.va.v.v_app.v.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background job to publish scheduled posts.
 * 
 * Runs every minute to check for posts whose `scheduledFor` time has passed and
 * are still marked as drafts. Publishes them by setting isDraft = false and
 * clearing the scheduledFor field.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "feature.social.post.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PostSchedulerJob {

    private final PostRepository postRepository;
    private final NotificationService notificationService;

    /**
     * Check every minute for scheduled posts that need to be published.
     */
    @Scheduled(cron = "${feature.social.post.scheduler.cron:0 * * * * *}")
    @Transactional
    public void publishScheduledPosts() {
        long startTime = System.currentTimeMillis();

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Post> postsToPublish = postRepository.findScheduledPostsDue(now);

            if (postsToPublish.isEmpty()) {
                return; // No noisy logs when nothing to do
            }

            log.info("📅 Post Scheduler: Found {} scheduled posts ready to publish", postsToPublish.size());

            int published = 0;
            for (Post post : postsToPublish) {
                try {
                    post.setIsDraft(false);
                    post.setScheduledFor(null);
                    postRepository.save(post);
                    published++;

                    log.info("✅ Published scheduled post ID: {} by user: {}", post.getId(), post.getUserId());
                } catch (Exception e) {
                    log.error("❌ Failed to publish scheduled post ID: {}", post.getId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("📅 Post Scheduler completed: {}/{} posts published in {}ms",
                    published, postsToPublish.size(), duration);

        } catch (Exception e) {
            log.error("❌ Error in Post Scheduler job", e);
        }
    }

    /**
     * Cleanup: Remove orphan drafts older than 30 days that were never scheduled.
     * Runs once daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupStaleDrafts() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            List<Post> staleDrafts = postRepository.findStaleDrafts(cutoff);

            if (staleDrafts.isEmpty())
                return;

            log.info("🗑️ Cleaning up {} stale drafts older than 30 days", staleDrafts.size());

            for (Post draft : staleDrafts) {
                draft.setIsDeleted(true);
                draft.setDeletedAt(LocalDateTime.now());
                postRepository.save(draft);
            }

            log.info("🗑️ Stale draft cleanup completed: {} drafts marked as deleted", staleDrafts.size());
        } catch (Exception e) {
            log.error("❌ Error in stale draft cleanup job", e);
        }
    }
}
