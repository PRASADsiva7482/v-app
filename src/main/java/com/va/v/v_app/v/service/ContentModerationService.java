package com.va.v.v_app.v.service;

import com.va.v.v_app.v.model.ContentFlag;
import com.va.v.v_app.v.repository.ContentFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final ContentFlagRepository contentFlagRepository;

    // Basic keyword-based moderation (production would use ML models)
    private static final List<String> HARMFUL_KEYWORDS = Arrays.asList(
            "hate", "violence", "threat", "harassment", "spam", "scam"
    );

    /**
     * AI-simulated content scan — checks text for harmful patterns.
     * Returns a ContentFlag if harmful content is detected, null otherwise.
     */
    public ContentFlag scanContent(String content, Long postId, Long commentId, Long messageId) {
        if (content == null || content.isBlank()) return null;

        String lower = content.toLowerCase();
        StringBuilder detectedCategories = new StringBuilder();
        double maxConfidence = 0.0;

        for (String keyword : HARMFUL_KEYWORDS) {
            if (lower.contains(keyword)) {
                if (detectedCategories.length() > 0) detectedCategories.append(",");
                detectedCategories.append(keyword.toUpperCase());
                maxConfidence = Math.max(maxConfidence, 0.7 + (Math.random() * 0.25));
            }
        }

        if (maxConfidence > 0) {
            ContentFlag flag = ContentFlag.builder()
                    .postId(postId)
                    .commentId(commentId)
                    .messageId(messageId)
                    .flagType("AI_DETECTED")
                    .reason("Automated content scan detected potentially harmful content")
                    .aiConfidence(Math.round(maxConfidence * 100.0) / 100.0)
                    .aiCategories(detectedCategories.toString())
                    .status("PENDING")
                    .build();

            log.info("AI moderation flagged content. Categories: {}, Confidence: {}",
                    detectedCategories, maxConfidence);

            return contentFlagRepository.save(flag);
        }
        return null;
    }

    /**
     * Manual user report
     */
    public ContentFlag reportContent(Long postId, String reportedBy, String flagType, String reason) {
        if (postId != null && contentFlagRepository.existsByPostIdAndReportedBy(postId, reportedBy)) {
            throw new RuntimeException("You have already reported this content");
        }

        ContentFlag flag = ContentFlag.builder()
                .postId(postId)
                .reportedBy(reportedBy)
                .flagType(flagType)
                .reason(reason)
                .status("PENDING")
                .build();

        return contentFlagRepository.save(flag);
    }

    public List<ContentFlag> getPendingFlags() {
        return contentFlagRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    public List<ContentFlag> getFlagsForPost(Long postId) {
        return contentFlagRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    public long getPendingFlagCount(Long postId) {
        return contentFlagRepository.countByPostIdAndStatus(postId, "PENDING");
    }
}
