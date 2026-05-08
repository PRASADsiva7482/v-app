package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Media entity
 */
@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByPost_Id(Long postId);

    List<Media> findByPostId(Long postId);

    void deleteByPost_Id(Long postId);
}
