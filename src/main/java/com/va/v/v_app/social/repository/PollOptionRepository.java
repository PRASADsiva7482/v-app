package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PollOption entity
 */
@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPollIdOrderByPositionAsc(Long pollId);

    @Modifying
    @Query("UPDATE PollOption po SET po.voteCount = po.voteCount + 1 WHERE po.id = :optionId")
    void incrementVoteCount(@Param("optionId") Long optionId);

    @Modifying
    @Query("UPDATE PollOption po SET po.voteCount = po.voteCount - 1 WHERE po.id = :optionId AND po.voteCount > 0")
    void decrementVoteCount(@Param("optionId") Long optionId);
}
