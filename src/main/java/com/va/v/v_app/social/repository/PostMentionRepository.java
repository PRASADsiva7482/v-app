package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.PostMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostMentionRepository extends JpaRepository<PostMention, Long> {
    List<PostMention> findByPostId(Long postId);

    List<PostMention> findByMentionedUserId(String userId);

    void deleteByPostId(Long postId);
}
