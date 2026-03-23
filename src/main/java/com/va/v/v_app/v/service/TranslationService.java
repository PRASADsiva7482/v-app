package com.va.v.v_app.v.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translation service for auto-translating post content.
 * 
 * Architecture:
 * - Uses an in-memory cache to avoid re-translating the same content
 * - Supports pluggable translation backends (LibreTranslate, Google, DeepL)
 * - Falls back to a no-op "original text" return if the backend is unavailable
 * 
 * To connect a real API:
 * 1. Set `feature.translation.api-url` to your LibreTranslate/Google endpoint
 * 2. Set `feature.translation.api-key` if required
 * 3. The service will automatically use the real API instead of the stub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    @Value("${feature.translation.api-url:}")
    private String translationApiUrl;

    @Value("${feature.translation.api-key:}")
    private String translationApiKey;

    @Value("${feature.translation.enabled:false}")
    private boolean translationEnabled;

    @Value("${feature.translation.cache-max-size:5000}")
    private int cacheMaxSize;

    // Simple in-memory translation cache: "content|sourceLang|targetLang" ->
    // translated
    private final ConcurrentHashMap<String, String> translationCache = new ConcurrentHashMap<>();

    /**
     * Translate text from source language to target language.
     *
     * @param text       The text to translate
     * @param sourceLang Source language code (e.g., "auto", "en", "es")
     * @param targetLang Target language code (e.g., "en", "hi", "fr")
     * @return Translated text, or original text if translation fails/unavailable
     */
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank())
            return text;
        if (!translationEnabled)
            return text;
        if (sourceLang != null && sourceLang.equals(targetLang))
            return text;

        // Check cache first
        String cacheKey = text.hashCode() + "|" + sourceLang + "|" + targetLang;
        String cached = translationCache.get(cacheKey);
        if (cached != null)
            return cached;

        try {
            String translated;

            if (translationApiUrl != null && !translationApiUrl.isBlank()) {
                // Use real translation API (LibreTranslate compatible)
                translated = callTranslationAPI(text, sourceLang, targetLang);
            } else {
                // Stub: Return original text with a language indicator
                // In production, this would be replaced by a real API call
                translated = text; // No-op fallback
                log.debug("Translation API not configured. Returning original text.");
            }

            // Cache the result
            if (translationCache.size() < cacheMaxSize) {
                translationCache.put(cacheKey, translated);
            }

            return translated;

        } catch (Exception e) {
            log.warn("Translation failed for text ({}→{}): {}", sourceLang, targetLang, e.getMessage());
            return text; // Graceful fallback
        }
    }

    /**
     * Detect the language of the given text.
     * Returns a language code (e.g., "en", "es", "hi") or "unknown".
     */
    public String detectLanguage(String text) {
        if (text == null || text.isBlank())
            return "unknown";

        if (translationApiUrl != null && !translationApiUrl.isBlank()) {
            try {
                return callLanguageDetectionAPI(text);
            } catch (Exception e) {
                log.warn("Language detection failed: {}", e.getMessage());
            }
        }

        // Simple heuristic fallback based on character ranges
        return detectLanguageByCharset(text);
    }

    /**
     * Translate a batch of texts efficiently.
     */
    public Map<Long, String> translateBatch(Map<Long, String> textsById, String targetLang) {
        Map<Long, String> results = new LinkedHashMap<>();

        for (Map.Entry<Long, String> entry : textsById.entrySet()) {
            String translated = translate(entry.getValue(), "auto", targetLang);
            results.put(entry.getKey(), translated);
        }

        return results;
    }

    /**
     * Clear the translation cache (useful for memory management).
     */
    public void clearCache() {
        translationCache.clear();
        log.info("Translation cache cleared");
    }

    public int getCacheSize() {
        return translationCache.size();
    }

    // ─── Private API Methods ───

    private String callTranslationAPI(String text, String sourceLang, String targetLang) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> request = new HashMap<>();
            request.put("q", text);
            request.put("source", sourceLang != null ? sourceLang : "auto");
            request.put("target", targetLang);

            if (translationApiKey != null && !translationApiKey.isBlank()) {
                request.put("api_key", translationApiKey);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    translationApiUrl + "/translate", request, Map.class);

            if (response != null && response.containsKey("translatedText")) {
                return (String) response.get("translatedText");
            }

            log.warn("Unexpected translation API response format");
            return text;

        } catch (Exception e) {
            log.error("Translation API call failed: {}", e.getMessage());
            return text;
        }
    }

    private String callLanguageDetectionAPI(String text) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> request = new HashMap<>();
            request.put("q", text);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> response = restTemplate.postForObject(
                    translationApiUrl + "/detect", request, List.class);

            if (response != null && !response.isEmpty()) {
                return (String) response.get(0).get("language");
            }
        } catch (Exception e) {
            log.debug("Language detection API call failed: {}", e.getMessage());
        }
        return "unknown";
    }

    /**
     * Simple character-range-based language detection fallback.
     */
    private String detectLanguageByCharset(String text) {
        int total = 0, latin = 0, devanagari = 0, cjk = 0, arabic = 0, korean = 0, telugu = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                total++;
                if (c >= '\u0041' && c <= '\u007A')
                    latin++;
                else if (c >= '\u0900' && c <= '\u097F')
                    devanagari++;
                else if (c >= '\u4E00' && c <= '\u9FFF')
                    cjk++;
                else if (c >= '\u0600' && c <= '\u06FF')
                    arabic++;
                else if (c >= '\uAC00' && c <= '\uD7AF')
                    korean++;
                else if (c >= '\u0C00' && c <= '\u0C7F')
                    telugu++;
            }
        }

        if (total == 0)
            return "unknown";

        double threshold = 0.3;
        if ((double) devanagari / total > threshold)
            return "hi";
        if ((double) cjk / total > threshold)
            return "zh";
        if ((double) arabic / total > threshold)
            return "ar";
        if ((double) korean / total > threshold)
            return "ko";
        if ((double) telugu / total > threshold)
            return "te";
        if ((double) latin / total > threshold)
            return "en"; // Approximation

        return "unknown";
    }
}
