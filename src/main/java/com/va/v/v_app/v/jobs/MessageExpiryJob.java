package com.va.v.v_app.v.jobs;

import com.va.v.v_app.v.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Background job to clean up expired self-destructing messages.
 * 
 * Runs every 30 seconds to mark expired messages as deleted.
 * This powers the "Vanish Mode" feature in DMs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "feature.chat.vanish-mode", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageExpiryJob {

    private final MessageRepository messageRepository;

    /**
     * Delete (soft-delete) messages that have passed their expiry time.
     */
    @Scheduled(fixedDelayString = "${feature.chat.vanish-mode.check-interval-ms:30000}")
    @Transactional
    public void cleanupExpiredMessages() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deleted = messageRepository.softDeleteExpiredMessages(now);

            if (deleted > 0) {
                log.info("💨 Vanish Mode: Soft-deleted {} expired messages", deleted);
            }
        } catch (Exception e) {
            log.error("❌ Error in message expiry cleanup job", e);
        }
    }
}
