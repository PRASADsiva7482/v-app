package com.va.v.v_app.social.repository;

import com.va.v.v_app.social.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PollVote entity
 */
@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    Optional<PollVote> findByPollIdAndUserId(Long pollId, String userId);

    boolean existsByPollIdAndUserId(Long pollId, String userId);

    // Batch loading for feed - check which polls user has voted on
    @Query("SELECT pv FROM PollVote pv WHERE pv.pollId IN :pollIds AND pv.userId = :userId")
    List<PollVote> findByPollIdInAndUserId(@Param("pollIds") List<Long> pollIds, @Param("userId") String userId);

    void deleteByPollId(Long pollId);

    void deleteByUserId(String userId);
}
