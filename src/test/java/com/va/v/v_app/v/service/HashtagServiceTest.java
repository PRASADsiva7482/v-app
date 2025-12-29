package com.va.v.v_app.v.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.va.v.v_app.v.repository.HashtagRepository;
import com.va.v.v_app.v.repository.PostHashtagRepository;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HashtagService - Hashtag extraction functionality
 */
@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private PostHashtagRepository postHashtagRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private HashtagService hashtagService;

    @Test
    void testExtractHashtags_WithValidHashtags() {
        // Given
        String content = "This is a post about #java #springboot and #mysql";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(3, hashtags.size());
        assertTrue(hashtags.contains("java"));
        assertTrue(hashtags.contains("springboot"));
        assertTrue(hashtags.contains("mysql"));
    }

    @Test
    void testExtractHashtags_WithMixedCase() {
        // Given
        String content = "#Java #SPRINGBOOT #MySQL";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(3, hashtags.size());
        assertTrue(hashtags.contains("java"));
        assertTrue(hashtags.contains("springboot"));
        assertTrue(hashtags.contains("mysql"));
    }

    @Test
    void testExtractHashtags_WithDuplicates() {
        // Given
        String content = "#java #Java #JAVA";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(1, hashtags.size());
        assertTrue(hashtags.contains("java"));
    }

    @Test
    void testExtractHashtags_WithNumbers() {
        // Given
        String content = "Using #java17 and #springboot3 for my project";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(2, hashtags.size());
        assertTrue(hashtags.contains("java17"));
        assertTrue(hashtags.contains("springboot3"));
    }

    @Test
    void testExtractHashtags_WithUnderscores() {
        // Given
        String content = "Learning #web_development and #software_engineering";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(2, hashtags.size());
        assertTrue(hashtags.contains("web_development"));
        assertTrue(hashtags.contains("software_engineering"));
    }

    @Test
    void testExtractHashtags_EmptyContent() {
        // Given
        String content = "";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertTrue(hashtags.isEmpty());
    }

    @Test
    void testExtractHashtags_NullContent() {
        // Given
        String content = null;

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertTrue(hashtags.isEmpty());
    }

    @Test
    void testExtractHashtags_NoHashtags() {
        // Given
        String content = "This is a post without any hashtags";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertTrue(hashtags.isEmpty());
    }

    @Test
    void testExtractHashtags_OnlyHashSymbol() {
        // Given
        String content = "Just a # symbol";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertTrue(hashtags.isEmpty());
    }

    @Test
    void testExtractHashtags_WithSpecialCharacters() {
        // Given
        String content = "#java-programming #web@dev #code!";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        // Only "java" should be extracted (special chars break the pattern)
        assertTrue(hashtags.contains("java"));
        assertTrue(hashtags.contains("web"));
        assertTrue(hashtags.contains("code"));
    }

    @Test
    void testExtractHashtags_MultipleInSentence() {
        // Given
        String content = "I'm learning #java and #python for #datascience projects!";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(3, hashtags.size());
        assertTrue(hashtags.contains("java"));
        assertTrue(hashtags.contains("python"));
        assertTrue(hashtags.contains("datascience"));
    }

    @Test
    void testExtractHashtags_WithEmojis() {
        // Given
        String content = "Loving #coding 💻 and #programming 🚀";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(2, hashtags.size());
        assertTrue(hashtags.contains("coding"));
        assertTrue(hashtags.contains("programming"));
    }

    @Test
    void testExtractHashtags_AtStartOfLine() {
        // Given
        String content = "#java is awesome";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(1, hashtags.size());
        assertTrue(hashtags.contains("java"));
    }

    @Test
    void testExtractHashtags_AtEndOfLine() {
        // Given
        String content = "I love programming #java";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(1, hashtags.size());
        assertTrue(hashtags.contains("java"));
    }

    @Test
    void testExtractHashtags_LongHashtag() {
        // Given - hashtag longer than 100 characters should be ignored
        String longTag = "a".repeat(101);
        String content = "#" + longTag + " #validtag";

        // When
        Set<String> hashtags = hashtagService.extractHashtags(content);

        // Then
        assertEquals(1, hashtags.size());
        assertTrue(hashtags.contains("validtag"));
        assertFalse(hashtags.contains(longTag));
    }
}
