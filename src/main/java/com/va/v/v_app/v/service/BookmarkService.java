package com.va.v.v_app.v.service;

import com.va.v.v_app.v.dto.response.PostResponse;
import com.va.v.v_app.v.exception.BusinessException;
import com.va.v.v_app.v.exception.ResourceNotFoundException;
import com.va.v.v_app.v.model.Bookmark;
import com.va.v.v_app.v.model.Post;
import com.va.v.v_app.v.repository.BookmarkRepository;
import com.va.v.v_app.v.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing bookmarks (saved posts).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    /**
     * Bookmark a post
     */
    @Transactional
    public void bookmarkPost(Long postId, String userId) {
        // Verify post exists
        postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Check if already bookmarked
        if (bookmarkRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BusinessException("ALREADY_BOOKMARKED", "Post is already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .postId(postId)
                .userId(userId)
                .build();

        bookmarkRepository.save(bookmark);
        log.info("User {} bookmarked post {}", userId, postId);
    }

    /**
     * Remove bookmark from a post
     */
    @Transactional
    public void unbookmarkPost(Long postId, String userId) {
        bookmarkRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new BusinessException("NOT_BOOKMARKED", "Post is not bookmarked"));

        bookmarkRepository.deleteByPostIdAndUserId(postId, userId);
        log.info("User {} removed bookmark from post {}", userId, postId);
    }

    /**
     * Check if a post is bookmarked by a user
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long postId, String userId) {
        return bookmarkRepository.existsByPostIdAndUserId(postId, userId);
    }

    /**
     * Get user's bookmarked posts
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getBookmarkedPosts(String userId, Pageable pageable) {
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return bookmarks.map(bookmark -> {
            Post post = postRepository.findByIdAndIsDeletedFalse(bookmark.getPostId()).orElse(null);
            if (post == null) return null;
            return postService.mapToResponse(post, userId);
        });
    }

    /**
     * Get user's bookmark count
     */
    @Transactional(readOnly = true)
    public long getBookmarkCount(String userId) {
        return bookmarkRepository.countByUserId(userId);
    }
}
