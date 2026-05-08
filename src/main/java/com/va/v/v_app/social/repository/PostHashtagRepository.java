package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.Hashtag;
import com.va.v.v_app.social.model.Post;
import com.va.v.v_app.social.model.PostHashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PostHashtag entity
 */
@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    List<PostHashtag> findByPost(Post post);

    List<PostHashtag> findByPostId(Long postId);

    List<PostHashtag> findByHashtag(Hashtag hashtag);

    void deleteByPost(Post post);

    void deleteByPostId(Long postId);

    // Get all hashtags for a specific post
    @Query("SELECT ph.hashtag FROM PostHashtag ph WHERE ph.post.id = :postId")
    List<Hashtag> findHashtagsByPostId(@Param("postId") Long postId);

    // Get all posts with a specific hashtag
    @Query("SELECT ph.post FROM PostHashtag ph WHERE ph.hashtag.id = :hashtagId AND ph.post.isDeleted = false ORDER BY ph.post.createdAt DESC")
    Page<Post> findPostsByHashtagId(@Param("hashtagId") Long hashtagId, Pageable pageable);

    // Get all posts with a specific hashtag name
    @Query("SELECT ph.post FROM PostHashtag ph WHERE ph.hashtag.tagName = :tagName AND ph.post.isDeleted = false ORDER BY ph.post.createdAt DESC")
    Page<Post> findPostsByHashtagName(@Param("tagName") String tagName, Pageable pageable);

    // Check if a post has a specific hashtag
    @Query("SELECT COUNT(ph) > 0 FROM PostHashtag ph WHERE ph.post.id = :postId AND ph.hashtag.id = :hashtagId")
    boolean existsByPostIdAndHashtagId(@Param("postId") Long postId, @Param("hashtagId") Long hashtagId);
}
