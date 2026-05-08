package com.va.v.v_app.social.api;

import com.va.v.v_app.social.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for content translation.
 * 
 * Endpoints:
 * - POST /api/v1/translate — Translate a text string
 * - POST /api/v1/translate/detect — Detect the language of a text
 */
@RestController
@RequestMapping("/api/v1/translate")
@RequiredArgsConstructor
@Tag(name = "Translation", description = "Content translation APIs")
public class TranslationController {

    private final TranslationService translationService;

    @Operation(summary = "Translate text to target language")
    @PostMapping
    public ResponseEntity<Map<String, Object>> translate(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        String sourceLang = request.getOrDefault("sourceLang", "auto");
        String targetLang = request.getOrDefault("targetLang", "en");

        String translated = translationService.translate(text, sourceLang, targetLang);
        String detectedLang = translationService.detectLanguage(text);

        return ResponseEntity.ok(Map.of(
                "originalText", text,
                "translatedText", translated,
                "sourceLang", detectedLang,
                "targetLang", targetLang,
                "translated", !text.equals(translated)));
    }

    @Operation(summary = "Detect the language of a text")
    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectLanguage(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        String language = translationService.detectLanguage(text);

        return ResponseEntity.ok(Map.of(
                "text", text.length() > 100 ? text.substring(0, 100) + "..." : text,
                "detectedLanguage", language));
    }
}
