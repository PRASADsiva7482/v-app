package com.va.v.v_app.v.service;

import com.va.v.v_app.v.model.ContentFlag;
import com.va.v.v_app.v.repository.ContentFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced AI-simulated content moderation service.
 * 
 * Upgrades from basic keyword matching to multi-layer analysis:
 * 1. Toxicity detection (expanded vocabulary + contextual patterns)
 * 2. Spam detection (repetition, excessive caps, link spam)
 * 3. Sensitive content classification (violence, hate speech, self-harm)
 * 4. PII detection (SSN, credit cards, phone numbers in content)
 * 5. Severity scoring with actionable recommendations
 * 
 * In production, replace the pattern-based approach with a real ML model
 * (e.g., Google Perspective API, OpenAI Moderation API, AWS Comprehend).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdvancedModerationService {

    private final ContentFlagRepository contentFlagRepository;

    // ─── Category Definitions ───

    private static final Map<String, List<String>> CATEGORY_PATTERNS = new LinkedHashMap<>() {
        {
            put("HATE_SPEECH", Arrays.asList(
                    "hate", "racist", "racism", "bigot", "slur", "supremacy", "xenophob",
                    "homophob", "transphob", "antisemit", "islamophob"));
            put("HARASSMENT", Arrays.asList(
                    "harass", "bully", "stalk", "intimidat", "threaten", "dox",
                    "cyberbully", "troll", "abuse"));
            put("VIOLENCE", Arrays.asList(
                    "kill", "murder", "assault", "attack", "bomb", "shoot",
                    "weapon", "terror", "violent"));
            put("SELF_HARM", Arrays.asList(
                    "suicide", "self-harm", "cutting", "overdose", "end my life",
                    "want to die", "kill myself"));
            put("SEXUAL_CONTENT", Arrays.asList(
                    "nsfw", "explicit", "pornograph", "nude"));
            put("SPAM", Arrays.asList(
                    "buy now", "click here", "free money", "earn cash",
                    "limited offer", "act now", "winner", "congratulations you",
                    "make money fast", "crypto invest"));
            put("MISINFORMATION", Arrays.asList(
                    "fake news", "hoax", "conspiracy", "debunked", "false claim"));
            put("SCAM", Arrays.asList(
                    "scam", "phishing", "ponzi", "pyramid scheme", "get rich quick",
                    "wire transfer", "send money", "nigerian prince"));
        }
    };

    // PII patterns
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");
    private static final Pattern PHONE_PATTERN = Pattern
            .compile("\\b(?:\\+?1?[-. ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    /**
     * Perform comprehensive multi-layer content analysis.
     * Returns a ModerationResult with severity, categories, and recommendations.
     */
    public ModerationResult analyzeContent(String content) {
        if (content == null || content.isBlank()) {
            return ModerationResult.safe();
        }

        String lower = content.toLowerCase();
        List<String> detectedCategories = new ArrayList<>();
        Map<String, Double> categoryConfidences = new LinkedHashMap<>();
        List<String> recommendations = new ArrayList<>();

        // Layer 1: Category-based pattern matching
        for (Map.Entry<String, List<String>> entry : CATEGORY_PATTERNS.entrySet()) {
            String category = entry.getKey();
            List<String> patterns = entry.getValue();
            int matches = 0;

            for (String pattern : patterns) {
                if (lower.contains(pattern)) {
                    matches++;
                }
            }

            if (matches > 0) {
                double confidence = Math.min(0.5 + (matches * 0.15), 0.98);
                detectedCategories.add(category);
                categoryConfidences.put(category, confidence);
            }
        }

        // Layer 2: Spam heuristics
        double spamScore = calculateSpamScore(content, lower);
        if (spamScore > 0.6 && !detectedCategories.contains("SPAM")) {
            detectedCategories.add("SPAM");
            categoryConfidences.put("SPAM", spamScore);
        }

        // Layer 3: PII detection
        List<String> piiTypes = detectPII(content);
        if (!piiTypes.isEmpty()) {
            detectedCategories.add("PII_EXPOSED");
            categoryConfidences.put("PII_EXPOSED", 0.95);
            recommendations.add("Content contains personal information (" +
                    String.join(", ", piiTypes) + "). Consider removing before posting.");
        }

        // Layer 4: Self-harm urgency check
        if (detectedCategories.contains("SELF_HARM")) {
            recommendations.add("URGENT: Content may indicate self-harm. " +
                    "Consider providing mental health resources (e.g., 988 Suicide & Crisis Lifeline).");
        }

        // Calculate overall severity
        double maxConfidence = categoryConfidences.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0);

        String severity;
        if (maxConfidence >= 0.85)
            severity = "CRITICAL";
        else if (maxConfidence >= 0.65)
            severity = "HIGH";
        else if (maxConfidence >= 0.45)
            severity = "MEDIUM";
        else if (maxConfidence > 0)
            severity = "LOW";
        else
            severity = "SAFE";

        // Generate action recommendation
        String action;
        if ("CRITICAL".equals(severity))
            action = "AUTO_REMOVE";
        else if ("HIGH".equals(severity))
            action = "HIDE_PENDING_REVIEW";
        else if ("MEDIUM".equals(severity))
            action = "FLAG_FOR_REVIEW";
        else
            action = "ALLOW";

        return ModerationResult.builder()
                .flagged(!detectedCategories.isEmpty())
                .severity(severity)
                .action(action)
                .categories(detectedCategories)
                .categoryConfidences(categoryConfidences)
                .overallConfidence(Math.round(maxConfidence * 100.0) / 100.0)
                .recommendations(recommendations)
                .build();
    }

    /**
     * Full scan: analyze content and persist to DB if flagged.
     */
    public ContentFlag scanAndPersist(String content, Long postId, Long commentId, Long messageId) {
        ModerationResult result = analyzeContent(content);

        if (!result.isFlagged())
            return null;

        ContentFlag flag = ContentFlag.builder()
                .postId(postId)
                .commentId(commentId)
                .messageId(messageId)
                .flagType("AI_DETECTED")
                .reason("Advanced AI moderation: " + result.getSeverity() +
                        " severity. Categories: " + String.join(", ", result.getCategories()))
                .aiConfidence(result.getOverallConfidence())
                .aiCategories(String.join(",", result.getCategories()))
                .status(determineInitialStatus(result))
                .build();

        log.info("🛡️ AI Moderation flagged content. Severity: {}, Categories: {}, Action: {}",
                result.getSeverity(), result.getCategories(), result.getAction());

        return contentFlagRepository.save(flag);
    }

    // ─── Helpers ───

    private double calculateSpamScore(String content, String lower) {
        double score = 0.0;

        // Excessive caps (more than 50% uppercase in content > 10 chars)
        if (content.length() > 10) {
            long upperCount = content.chars().filter(Character::isUpperCase).count();
            if ((double) upperCount / content.length() > 0.5) {
                score += 0.3;
            }
        }

        // Excessive exclamation/question marks
        long exclamationCount = content.chars().filter(c -> c == '!' || c == '?').count();
        if (exclamationCount > 5)
            score += 0.2;

        // Repetitive characters (e.g., "aaaaaa")
        if (Pattern.compile("(.)\\1{4,}").matcher(lower).find()) {
            score += 0.2;
        }

        // Multiple URLs
        Matcher urlMatcher = URL_PATTERN.matcher(content);
        int urlCount = 0;
        while (urlMatcher.find())
            urlCount++;
        if (urlCount > 3)
            score += 0.3;

        // Very short content with links (likely spam)
        if (content.length() < 50 && urlCount > 0)
            score += 0.15;

        return Math.min(score, 0.98);
    }

    private List<String> detectPII(String content) {
        List<String> piiTypes = new ArrayList<>();

        if (SSN_PATTERN.matcher(content).find())
            piiTypes.add("SSN");
        if (CREDIT_CARD_PATTERN.matcher(content).find())
            piiTypes.add("Credit Card");

        // Only flag phone numbers if there are multiple (single phone in bio is common)
        Matcher phoneMatcher = PHONE_PATTERN.matcher(content);
        int phoneCount = 0;
        while (phoneMatcher.find())
            phoneCount++;
        if (phoneCount > 1)
            piiTypes.add("Phone Numbers");

        // Flag emails if any match
        if (EMAIL_PATTERN.matcher(content).find())
            piiTypes.add("Email Address");

        return piiTypes;
    }

    private String determineInitialStatus(ModerationResult result) {
        if ("AUTO_REMOVE".equals(result.getAction()))
            return "AUTO_REMOVED";
        if ("HIDE_PENDING_REVIEW".equals(result.getAction()))
            return "HIDDEN";
        return "PENDING";
    }

    // ─── Result DTO ───

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModerationResult {
        private boolean flagged;
        private String severity; // SAFE, LOW, MEDIUM, HIGH, CRITICAL
        private String action; // ALLOW, FLAG_FOR_REVIEW, HIDE_PENDING_REVIEW, AUTO_REMOVE
        private List<String> categories;
        private Map<String, Double> categoryConfidences;
        private double overallConfidence;
        private List<String> recommendations;

        public static ModerationResult safe() {
            return ModerationResult.builder()
                    .flagged(false)
                    .severity("SAFE")
                    .action("ALLOW")
                    .categories(Collections.emptyList())
                    .categoryConfidences(Collections.emptyMap())
                    .overallConfidence(0.0)
                    .recommendations(Collections.emptyList())
                    .build();
        }
    }
}
